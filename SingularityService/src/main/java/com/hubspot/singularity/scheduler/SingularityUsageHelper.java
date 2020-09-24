package com.hubspot.singularity.scheduler;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.client.MesosClient;
import com.hubspot.mesos.json.MesosAgentMetricsSnapshotObject;
import com.hubspot.mesos.json.MesosTaskMonitorObject;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.InvalidSingularityTaskIdException;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.RequestUtilization;
import com.hubspot.singularity.SingularityAgent;
import com.hubspot.singularity.SingularityAgentUsage;
import com.hubspot.singularity.SingularityAgentUsageWithId;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCurrentUsage;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.AgentManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.ShuffleConfigurationManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.usage.UsageManager;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SingularityUsageHelper {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityUsageHelper.class);
  private static final long DAY_IN_SECONDS = TimeUnit.DAYS.toSeconds(1);

  private final MesosClient mesosClient;
  private final SingularityConfiguration configuration;
  private final SingularityExceptionNotifier exceptionNotifier;
  private final RequestManager requestManager;
  private final AgentManager agentManager;
  private final TaskManager taskManager;
  private final UsageManager usageManager;
  private final ShuffleConfigurationManager shuffleConfigurationManager;

  @Inject
  public SingularityUsageHelper(
    MesosClient mesosClient,
    SingularityConfiguration configuration,
    SingularityExceptionNotifier exceptionNotifier,
    RequestManager requestManager,
    AgentManager agentManager,
    TaskManager taskManager,
    UsageManager usageManager,
    ShuffleConfigurationManager shuffleConfigurationManager
  ) {
    this.mesosClient = mesosClient;
    this.configuration = configuration;
    this.exceptionNotifier = exceptionNotifier;
    this.requestManager = requestManager;
    this.agentManager = agentManager;
    this.taskManager = taskManager;
    this.usageManager = usageManager;
    this.shuffleConfigurationManager = shuffleConfigurationManager;
  }

  public List<SingularityAgent> getAgentsToTrackUsageFor() {
    List<SingularityAgent> agents = agentManager.getObjects();
    List<SingularityAgent> agentsToTrack = new ArrayList<>(agents.size());

    for (SingularityAgent agent : agents) {
      if (
        agent.getCurrentState().getState().isInactive() ||
        agent.getCurrentState().getState() == MachineState.DECOMMISSIONED
      ) {
        continue;
      }

      agentsToTrack.add(agent);
    }

    return agentsToTrack;
  }

  public MesosAgentMetricsSnapshotObject getMetricsSnapshot(String host) {
    return mesosClient.getAgentMetricsSnapshot(host, true);
  }

  public void collectAgentUsage(
    SingularityAgent agent,
    long now,
    Map<String, RequestUtilization> utilizationPerRequestId,
    Map<String, RequestUtilization> previousUtilizations,
    Map<SingularityAgentUsage, List<TaskIdWithUsage>> overLoadedHosts,
    AtomicLong totalMemBytesUsed,
    AtomicLong totalMemBytesAvailable,
    AtomicDouble totalCpuUsed,
    AtomicDouble totalCpuAvailable,
    AtomicLong totalDiskBytesUsed,
    AtomicLong totalDiskBytesAvailable,
    boolean useShortTimeout
  ) {
    Optional<Long> memoryMbTotal = Optional.empty();
    Optional<Double> cpusTotal = Optional.empty();
    Optional<Long> diskMbTotal = Optional.empty();

    long memoryMbReserved = 0;
    double cpuReserved = 0;
    long diskMbReserved = 0;

    long memoryBytesUsed = 0;
    double cpusUsed = 0;
    long diskMbUsed = 0;

    try {
      List<MesosTaskMonitorObject> allTaskUsage = mesosClient.getAgentResourceUsage(
        agent.getHost(),
        useShortTimeout
      );
      MesosAgentMetricsSnapshotObject agentMetricsSnapshot = mesosClient.getAgentMetricsSnapshot(
        agent.getHost()
      );
      double systemMemTotalBytes = 0;
      double systemMemFreeBytes = 0;
      double systemLoad1Min = 0;
      double systemLoad5Min = 0;
      double systemLoad15Min = 0;
      double diskUsed = 0;
      double diskTotal = 0;
      double systemCpusTotal = 0;
      if (agentMetricsSnapshot != null) {
        systemMemTotalBytes = agentMetricsSnapshot.getSystemMemTotalBytes();
        systemMemFreeBytes = agentMetricsSnapshot.getSystemMemFreeBytes();
        systemLoad1Min = agentMetricsSnapshot.getSystemLoad1Min();
        systemLoad5Min = agentMetricsSnapshot.getSystemLoad5Min();
        systemLoad15Min = agentMetricsSnapshot.getSystemLoad15Min();
        diskUsed = agentMetricsSnapshot.getDiskUsed();
        diskTotal = agentMetricsSnapshot.getDiskTotal();
        systemCpusTotal = agentMetricsSnapshot.getSystemCpusTotal();
      }

      double systemLoad;
      switch (configuration.getMesosConfiguration().getScoreUsingSystemLoad()) {
        case LOAD_1:
          systemLoad = systemLoad1Min;
          break;
        case LOAD_15:
          systemLoad = systemLoad15Min;
          break;
        case LOAD_5:
        default:
          systemLoad = systemLoad5Min;
          break;
      }

      boolean overloadedForCpu =
        systemCpusTotal > 0 && systemLoad / systemCpusTotal > 1.0;
      boolean experiencingHighMemUsage =
        ((systemMemTotalBytes - systemMemFreeBytes) / systemMemTotalBytes) >
        configuration.getShuffleTasksWhenAgentMemoryUtilizationPercentageExceeds();
      List<TaskIdWithUsage> possibleTasksToShuffle = new ArrayList<>();
      Set<String> shuffleBlacklist = new HashSet<>(
        shuffleConfigurationManager.getShuffleBlocklist()
      );

      for (MesosTaskMonitorObject taskUsage : allTaskUsage) {
        if (
          !taskUsage
            .getFrameworkId()
            .equals(configuration.getMesosConfiguration().getFrameworkId())
        ) {
          LOG.info(
            "Skipping task {} from other framework {}",
            taskUsage.getSource(),
            taskUsage.getFrameworkId()
          );
          continue;
        }
        String taskId = taskUsage.getSource();
        SingularityTaskId task;
        try {
          task = SingularityTaskId.valueOf(taskId);
        } catch (InvalidSingularityTaskIdException e) {
          LOG.warn("Couldn't get SingularityTaskId for {}", taskUsage);
          continue;
        }

        SingularityTaskUsage latestUsage = getUsage(taskUsage);
        List<SingularityTaskUsage> pastTaskUsages = usageManager.getTaskUsage(task);
        usageManager.saveSpecificTaskUsage(task, latestUsage);

        Optional<SingularityTask> maybeTask = taskManager.getTask(task);
        Optional<Resources> maybeResources = Optional.empty();
        if (maybeTask.isPresent()) {
          maybeResources =
            maybeTask.get().getTaskRequest().getPendingTask().getResources().isPresent()
              ? maybeTask.get().getTaskRequest().getPendingTask().getResources()
              : maybeTask.get().getTaskRequest().getDeploy().getResources();
          if (maybeResources.isPresent()) {
            Resources taskResources = maybeResources.get();
            double memoryMbReservedForTask = taskResources.getMemoryMb();
            double cpuReservedForTask = taskResources.getCpus();
            double diskMbReservedForTask = taskResources.getDiskMb();

            memoryMbReserved += memoryMbReservedForTask;
            cpuReserved += cpuReservedForTask;
            diskMbReserved += diskMbReservedForTask;

            updateRequestUtilization(
              utilizationPerRequestId,
              previousUtilizations.get(
                maybeTask.get().getTaskRequest().getRequest().getId()
              ),
              pastTaskUsages,
              latestUsage,
              task,
              memoryMbReservedForTask,
              cpuReservedForTask,
              diskMbReservedForTask
            );
          }
        }
        memoryBytesUsed += latestUsage.getMemoryTotalBytes();
        diskMbUsed += latestUsage.getDiskTotalBytes();

        SingularityTaskCurrentUsage currentUsage = null;
        if (pastTaskUsages.isEmpty()) {
          Optional<SingularityTaskHistoryUpdate> maybeStartingUpdate = taskManager.getTaskHistoryUpdate(
            task,
            ExtendedTaskState.TASK_STARTING
          );
          if (maybeStartingUpdate.isPresent()) {
            long startTimestamp = maybeStartingUpdate.get().getTimestamp();
            double usedCpusSinceStart =
              latestUsage.getCpuSeconds() /
              TimeUnit.MILLISECONDS.toSeconds(
                latestUsage.getTimestamp() - startTimestamp
              );
            currentUsage =
              new SingularityTaskCurrentUsage(
                latestUsage.getMemoryTotalBytes(),
                (long) taskUsage.getStatistics().getTimestamp() * 1000,
                usedCpusSinceStart,
                latestUsage.getDiskTotalBytes()
              );

            cpusUsed += usedCpusSinceStart;
          }
        } else {
          SingularityTaskUsage lastUsage = pastTaskUsages.get(pastTaskUsages.size() - 1);

          double taskCpusUsed =
            (
              (latestUsage.getCpuSeconds() - lastUsage.getCpuSeconds()) /
              TimeUnit.MILLISECONDS.toSeconds(
                latestUsage.getTimestamp() - lastUsage.getTimestamp()
              )
            );

          currentUsage =
            new SingularityTaskCurrentUsage(
              latestUsage.getMemoryTotalBytes(),
              (long) taskUsage.getStatistics().getTimestamp() * 1000,
              taskCpusUsed,
              latestUsage.getDiskTotalBytes()
            );
          cpusUsed += taskCpusUsed;
        }

        if (currentUsage != null && currentUsage.getCpusUsed() > 0) {
          if (isEligibleForShuffle(task, shuffleBlacklist)) {
            Optional<SingularityTaskHistoryUpdate> maybeCleanupUpdate = taskManager.getTaskHistoryUpdate(
              task,
              ExtendedTaskState.TASK_CLEANING
            );
            if (
              maybeCleanupUpdate.isPresent() &&
              isTaskAlreadyCleanedUpForShuffle(maybeCleanupUpdate.get())
            ) {
              LOG.trace(
                "Task {} already being cleaned up to spread cpu or mem usage, skipping",
                taskId
              );
            } else {
              if (maybeResources.isPresent()) {
                possibleTasksToShuffle.add(
                  new TaskIdWithUsage(task, maybeResources.get(), currentUsage)
                );
              }
            }
          }
        }
      }

      if (
        !agent.getResources().isPresent() ||
        !agent.getResources().get().getMemoryMegaBytes().isPresent() ||
        !agent.getResources().get().getNumCpus().isPresent()
      ) {
        LOG.debug("Could not find agent or resources for agent {}", agent.getId());
      } else {
        memoryMbTotal =
          Optional.of(agent.getResources().get().getMemoryMegaBytes().get().longValue());
        cpusTotal =
          Optional.of(agent.getResources().get().getNumCpus().get().doubleValue());
        diskMbTotal = Optional.of(agent.getResources().get().getDiskSpace().get());
      }

      SingularityAgentUsage agentUsage = new SingularityAgentUsage(
        cpusUsed,
        cpuReserved,
        cpusTotal,
        memoryBytesUsed,
        memoryMbReserved,
        memoryMbTotal,
        diskMbUsed,
        diskMbReserved,
        diskMbTotal,
        allTaskUsage.size(),
        now,
        systemMemTotalBytes,
        systemMemFreeBytes,
        systemCpusTotal,
        systemLoad1Min,
        systemLoad5Min,
        systemLoad15Min,
        diskUsed,
        diskTotal
      );

      if (overloadedForCpu || experiencingHighMemUsage) {
        overLoadedHosts.put(agentUsage, possibleTasksToShuffle);
      }

      if (
        agentUsage.getMemoryBytesTotal().isPresent() &&
        agentUsage.getCpusTotal().isPresent()
      ) {
        totalMemBytesUsed.getAndAdd((long) agentUsage.getMemoryBytesUsed());
        totalCpuUsed.getAndAdd(agentUsage.getCpusUsed());
        totalDiskBytesUsed.getAndAdd((long) agentUsage.getDiskBytesUsed());

        totalMemBytesAvailable.getAndAdd(agentUsage.getMemoryBytesTotal().get());
        totalCpuAvailable.getAndAdd(agentUsage.getCpusTotal().get());
        totalDiskBytesAvailable.getAndAdd(agentUsage.getDiskBytesTotal().get());
      }

      LOG.debug("Saving agent {} usage {}", agent.getHost(), agentUsage);
      usageManager.saveCurrentAgentUsage(
        new SingularityAgentUsageWithId(agentUsage, agent.getId())
      );
    } catch (Throwable t) {
      String message = String.format(
        "Could not get agent usage for host %s",
        agent.getHost()
      );
      LOG.error(message, t);
      exceptionNotifier.notify(message, t);
    }
  }

  private SingularityTaskUsage getUsage(MesosTaskMonitorObject taskUsage) {
    double timestampMillis = taskUsage.getStatistics().getTimestamp() * 1000;
    return new SingularityTaskUsage(
      taskUsage.getStatistics().getMemTotalBytes(),
      (long) timestampMillis,
      taskUsage.getStatistics().getCpusSystemTimeSecs() +
      taskUsage.getStatistics().getCpusUserTimeSecs(),
      taskUsage.getStatistics().getDiskUsedBytes(),
      taskUsage.getStatistics().getCpusNrPeriods(),
      taskUsage.getStatistics().getCpusNrThrottled(),
      taskUsage.getStatistics().getCpusThrottledTimeSecs()
    );
  }

  private List<SingularityTaskUsage> getFullListOfTaskUsages(
    List<SingularityTaskUsage> pastTaskUsages,
    SingularityTaskUsage latestUsage,
    SingularityTaskId task
  ) {
    List<SingularityTaskUsage> pastTaskUsagesCopy = new ArrayList<>();
    pastTaskUsagesCopy.add(
      new SingularityTaskUsage(0, task.getStartedAt(), 0, 0, 0, 0, 0)
    ); // to calculate oldest cpu usage
    pastTaskUsagesCopy.addAll(pastTaskUsages);
    pastTaskUsagesCopy.add(latestUsage);

    return pastTaskUsagesCopy;
  }

  private boolean isEligibleForShuffle(
    SingularityTaskId task,
    Set<String> requestBlacklist
  ) {
    Optional<SingularityTaskHistoryUpdate> taskRunning = taskManager.getTaskHistoryUpdate(
      task,
      ExtendedTaskState.TASK_RUNNING
    );

    return (
      (
        !requestBlacklist.contains(task.getRequestId()) &&
        !configuration.getDoNotShuffleRequests().contains(task.getRequestId())
      ) &&
      isLongRunning(task) &&
      (
        configuration.getMinutesBeforeNewTaskEligibleForShuffle() == 0 || // Shuffle delay is disabled entirely
        (
          taskRunning.isPresent() &&
          TimeUnit.MILLISECONDS.toMinutes(
            System.currentTimeMillis() - taskRunning.get().getTimestamp()
          ) >=
          configuration.getMinutesBeforeNewTaskEligibleForShuffle()
        )
      )
    );
  }

  private boolean isLongRunning(SingularityTaskId task) {
    Optional<SingularityRequestWithState> request = requestManager.getRequest(
      task.getRequestId()
    );
    if (request.isPresent()) {
      return request.get().getRequest().getRequestType().isLongRunning();
    }

    LOG.warn(
      "Couldn't find request id {} for task {}",
      task.getRequestId(),
      task.getId()
    );
    return false;
  }

  private boolean isTaskAlreadyCleanedUpForShuffle(
    SingularityTaskHistoryUpdate taskHistoryUpdate
  ) {
    String statusMessage = taskHistoryUpdate.getStatusMessage().orElse("");
    if (
      statusMessage.contains(TaskCleanupType.REBALANCE_CPU_USAGE.name()) ||
      statusMessage.contains(TaskCleanupType.REBALANCE_MEMORY_USAGE.name())
    ) {
      return true;
    }
    for (SingularityTaskHistoryUpdate previous : taskHistoryUpdate.getPrevious()) {
      statusMessage = previous.getStatusMessage().orElse("");
      if (
        statusMessage.contains(TaskCleanupType.REBALANCE_CPU_USAGE.name()) ||
        statusMessage.contains(TaskCleanupType.REBALANCE_MEMORY_USAGE.name())
      ) {
        return true;
      }
    }
    return false;
  }

  private void updateRequestUtilization(
    Map<String, RequestUtilization> utilizationPerRequestId,
    RequestUtilization previous,
    List<SingularityTaskUsage> pastTaskUsages,
    SingularityTaskUsage latestUsage,
    SingularityTaskId task,
    double memoryMbReservedForTask,
    double cpuReservedForTask,
    double diskMbReservedForTask
  ) {
    String requestId = task.getRequestId();
    RequestUtilization newRequestUtilization = utilizationPerRequestId.getOrDefault(
      requestId,
      new RequestUtilization(requestId, task.getDeployId())
    );
    // Take the previous request utilization into account to better measure 24 hour max/min values
    if (previous != null) {
      if (previous.getMaxMemTimestamp() < DAY_IN_SECONDS) {
        newRequestUtilization.setMaxMemBytesUsed(previous.getMaxMemBytesUsed());
        newRequestUtilization.setMaxMemTimestamp(previous.getMaxMemTimestamp());
      }
      if (previous.getMinMemTimestamp() < DAY_IN_SECONDS) {
        newRequestUtilization.setMinMemBytesUsed(previous.getMinMemBytesUsed());
        newRequestUtilization.setMinMemTimestamp(previous.getMinMemTimestamp());
      }
      if (previous.getMaxCpusTimestamp() < DAY_IN_SECONDS) {
        newRequestUtilization.setMaxCpuUsed(previous.getMaxCpuUsed());
        newRequestUtilization.setMaxCpusTimestamp(previous.getMaxCpusTimestamp());
      }
      if (previous.getMinCpusTimestamp() < DAY_IN_SECONDS) {
        newRequestUtilization.setMinCpuUsed(previous.getMinCpuUsed());
        newRequestUtilization.setMinCpusTimestamp(previous.getMinCpusTimestamp());
      }
      if (previous.getMaxDiskTimestamp() < DAY_IN_SECONDS) {
        newRequestUtilization.setMaxDiskBytesUsed(previous.getMaxDiskBytesUsed());
        newRequestUtilization.setMaxDiskTimestamp(previous.getMaxDiskTimestamp());
      }
      if (previous.getMinDiskTimestamp() < DAY_IN_SECONDS) {
        newRequestUtilization.setMinDiskBytesUsed(previous.getMinDiskBytesUsed());
        newRequestUtilization.setMinDiskTimestamp(previous.getMinDiskTimestamp());
      }
      if (previous.getMaxCpuThrottledTimestamp() < DAY_IN_SECONDS) {
        newRequestUtilization.setMaxPercentCpuTimeThrottled(
          previous.getMaxPercentCpuTimeThrottled()
        );
        newRequestUtilization.setMaxCpuThrottledTimestamp(
          previous.getMaxCpuThrottledTimestamp()
        );
      }
      if (previous.getMinCpuThrottledTimestamp() < DAY_IN_SECONDS) {
        newRequestUtilization.setMinPercentCpuTimeThrottled(
          previous.getMinPercentCpuTimeThrottled()
        );
        newRequestUtilization.setMinCpuThrottledTimestamp(
          previous.getMinCpuThrottledTimestamp()
        );
      }
    }

    List<SingularityTaskUsage> pastTaskUsagesCopy = getFullListOfTaskUsages(
      pastTaskUsages,
      latestUsage,
      task
    );
    pastTaskUsagesCopy.sort(
      Comparator.comparingDouble(SingularityTaskUsage::getTimestamp)
    );
    int numTasks = pastTaskUsagesCopy.size() - 1; // One usage is a fake 0 usage to calculate first cpu times

    int numCpuOverages = 0;

    for (int i = 0; i < numTasks; i++) {
      SingularityTaskUsage olderUsage = pastTaskUsagesCopy.get(i);
      SingularityTaskUsage newerUsage = pastTaskUsagesCopy.get(i + 1);
      double timeElapsed = (double) (
        newerUsage.getTimestamp() - olderUsage.getTimestamp()
      ) /
      1000;
      double cpusUsed =
        (newerUsage.getCpuSeconds() - olderUsage.getCpuSeconds()) / timeElapsed;
      double percentCpuTimeThrottled =
        (newerUsage.getCpusThrottledTimeSecs() - olderUsage.getCpusThrottledTimeSecs()) /
        timeElapsed;

      if (cpusUsed > newRequestUtilization.getMaxCpuUsed()) {
        newRequestUtilization.setMaxCpuUsed(cpusUsed);
        newRequestUtilization.setMaxCpusTimestamp(newerUsage.getTimestamp());
      }
      if (cpusUsed < newRequestUtilization.getMinCpuUsed()) {
        newRequestUtilization.setMinCpuUsed(cpusUsed);
        newRequestUtilization.setMinCpusTimestamp(newerUsage.getTimestamp());
      }
      if (newerUsage.getMemoryTotalBytes() > newRequestUtilization.getMaxMemBytesUsed()) {
        newRequestUtilization.setMaxMemBytesUsed(newerUsage.getMemoryTotalBytes());
        newRequestUtilization.setMaxMemTimestamp(newerUsage.getTimestamp());
      }
      if (newerUsage.getMemoryTotalBytes() < newRequestUtilization.getMinMemBytesUsed()) {
        newRequestUtilization.setMinMemBytesUsed(newerUsage.getMemoryTotalBytes());
        newRequestUtilization.setMinMemTimestamp(newerUsage.getTimestamp());
      }
      if (newerUsage.getDiskTotalBytes() > newRequestUtilization.getMaxDiskBytesUsed()) {
        newRequestUtilization.setMaxDiskBytesUsed(newerUsage.getDiskTotalBytes());
        newRequestUtilization.setMaxDiskTimestamp(newerUsage.getTimestamp());
      }
      if (newerUsage.getDiskTotalBytes() < newRequestUtilization.getMinDiskBytesUsed()) {
        newRequestUtilization.setMinDiskBytesUsed(newerUsage.getDiskTotalBytes());
        newRequestUtilization.setMaxDiskTimestamp(newerUsage.getTimestamp());
      }
      if (
        percentCpuTimeThrottled > newRequestUtilization.getMaxPercentCpuTimeThrottled()
      ) {
        newRequestUtilization.setMaxPercentCpuTimeThrottled(percentCpuTimeThrottled);
        newRequestUtilization.setMaxCpuThrottledTimestamp(newerUsage.getTimestamp());
      }
      if (
        percentCpuTimeThrottled < newRequestUtilization.getMinPercentCpuTimeThrottled()
      ) {
        newRequestUtilization.setMinPercentCpuTimeThrottled(percentCpuTimeThrottled);
        newRequestUtilization.setMinCpuThrottledTimestamp(newerUsage.getTimestamp());
      }

      if (cpusUsed > cpuReservedForTask) {
        numCpuOverages++;
      }

      newRequestUtilization
        .addCpuUsed(cpusUsed)
        .addMemBytesUsed(newerUsage.getMemoryTotalBytes())
        .addPercentCpuTimeThrottled(percentCpuTimeThrottled)
        .addDiskBytesUsed(newerUsage.getDiskTotalBytes())
        .incrementTaskCount();
    }

    double cpuBurstRating = pastTaskUsagesCopy.size() > 0
      ? numCpuOverages / (double) pastTaskUsagesCopy.size()
      : 1;

    newRequestUtilization
      .addMemBytesReserved(
        (long) (
          memoryMbReservedForTask * SingularityAgentUsage.BYTES_PER_MEGABYTE * numTasks
        )
      )
      .addCpuReserved(cpuReservedForTask * numTasks)
      .addDiskBytesReserved(
        (long) diskMbReservedForTask * SingularityAgentUsage.BYTES_PER_MEGABYTE * numTasks
      )
      .setCpuBurstRating(cpuBurstRating);

    utilizationPerRequestId.put(requestId, newRequestUtilization);
  }
}
