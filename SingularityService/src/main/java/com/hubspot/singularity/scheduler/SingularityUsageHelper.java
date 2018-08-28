package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.client.MesosClient;
import com.hubspot.mesos.json.MesosSlaveMetricsSnapshotObject;
import com.hubspot.mesos.json.MesosTaskMonitorObject;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.InvalidSingularityTaskIdException;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.RequestUtilization;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCurrentUsage;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.UsageManager;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

@Singleton
public class SingularityUsageHelper {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityUsageHelper.class);
  private static final long DAY_IN_SECONDS = TimeUnit.DAYS.toSeconds(1);

  private final MesosClient mesosClient;
  private final SingularityConfiguration configuration;
  private final SingularityExceptionNotifier exceptionNotifier;
  private final RequestManager requestManager;
  private final SlaveManager slaveManager;
  private final TaskManager taskManager;
  private final UsageManager usageManager;

  private final ConcurrentHashMap<String, ReentrantLock> requestLocks;

  @Inject
  public SingularityUsageHelper(
      MesosClient mesosClient,
      SingularityConfiguration configuration,
      SingularityExceptionNotifier exceptionNotifier,
      RequestManager requestManager,
      SlaveManager slaveManager,
      TaskManager taskManager,
      UsageManager usageManager) {
    this.mesosClient = mesosClient;
    this.configuration = configuration;
    this.exceptionNotifier = exceptionNotifier;
    this.requestManager = requestManager;
    this.slaveManager = slaveManager;
    this.taskManager = taskManager;
    this.usageManager = usageManager;

    this.requestLocks = new ConcurrentHashMap<>();
  }

  public Set<String> getSlaveIdsToTrackUsageFor() {
    List<SingularitySlave> slaves = getSlavesToTrackUsageFor();
    Set<String> slaveIds = new HashSet<>(slaves.size());
    for (SingularitySlave slave : slaves) {
      slaveIds.add(slave.getId());
    }
    return slaveIds;
  }

  public List<SingularitySlave> getSlavesToTrackUsageFor() {
    List<SingularitySlave> slaves = slaveManager.getObjects();
    List<SingularitySlave> slavesToTrack = new ArrayList<>(slaves.size());

    for (SingularitySlave slave : slaves) {
      if (slave.getCurrentState().getState().isInactive() || slave.getCurrentState().getState() == MachineState.DECOMMISSIONED) {
        continue;
      }

      slavesToTrack.add(slave);
    }

    return slavesToTrack;
  }

  public MesosSlaveMetricsSnapshotObject getMetricsSnapshot(String host) {
    return mesosClient.getSlaveMetricsSnapshot(host, true);
  }

  public void collectSlaveUsage(
      SingularitySlave slave,
      long now,
      Map<String, RequestUtilization> utilizationPerRequestId,
      Map<String, RequestUtilization> previousUtilizations,
      Map<SingularitySlaveUsage, List<TaskIdWithUsage>> overLoadedHosts,
      AtomicLong totalMemBytesUsed,
      AtomicLong totalMemBytesAvailable,
      AtomicDouble totalCpuUsed,
      AtomicDouble totalCpuAvailable,
      AtomicLong totalDiskBytesUsed,
      AtomicLong totalDiskBytesAvailable,
      boolean useShortTimeout) {
    Optional<Long> memoryMbTotal = Optional.absent();
    Optional<Double> cpusTotal = Optional.absent();
    Optional<Long> diskMbTotal = Optional.absent();

    long memoryMbReservedOnSlave = 0;
    double cpuReservedOnSlave = 0;
    long diskMbReservedOnSlave = 0;

    long memoryBytesUsedOnSlave = 0;
    double cpusUsedOnSlave = 0;
    long diskMbUsedOnSlave = 0;

    try {
      List<MesosTaskMonitorObject> allTaskUsage = mesosClient.getSlaveResourceUsage(slave.getHost(), useShortTimeout);
      MesosSlaveMetricsSnapshotObject slaveMetricsSnapshot = mesosClient.getSlaveMetricsSnapshot(slave.getHost());
      double systemMemTotalBytes = 0;
      double systemMemFreeBytes = 0;
      double systemLoad1Min = 0;
      double systemLoad5Min = 0;
      double systemLoad15Min = 0;
      double slaveDiskUsed = 0;
      double slaveDiskTotal = 0;
      double systemCpusTotal = 0;
      if (slaveMetricsSnapshot != null) {
        systemMemTotalBytes = slaveMetricsSnapshot.getSystemMemTotalBytes();
        systemMemFreeBytes = slaveMetricsSnapshot.getSystemMemFreeBytes();
        systemLoad1Min = slaveMetricsSnapshot.getSystemLoad1Min();
        systemLoad5Min = slaveMetricsSnapshot.getSystemLoad5Min();
        systemLoad15Min = slaveMetricsSnapshot.getSystemLoad15Min();
        slaveDiskUsed = slaveMetricsSnapshot.getSlaveDiskUsed();
        slaveDiskTotal = slaveMetricsSnapshot.getSlaveDiskTotal();
        systemCpusTotal = slaveMetricsSnapshot.getSystemCpusTotal();
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

      boolean slaveOverloaded = systemCpusTotal > 0 && systemLoad / systemCpusTotal > 1.0;
      List<TaskIdWithUsage> possibleTasksToShuffle = new ArrayList<>();

      for (MesosTaskMonitorObject taskUsage : allTaskUsage) {
        if (!taskUsage.getFrameworkId().equals(configuration.getMesosConfiguration().getFrameworkId())) {
          LOG.info("Skipping task {} from other framework {}", taskUsage.getSource(), taskUsage.getFrameworkId());
          continue;
        }
        String taskId = taskUsage.getSource();
        SingularityTaskId task;
        try {
          task = SingularityTaskId.valueOf(taskId);
        } catch (InvalidSingularityTaskIdException e) {
          LOG.error("Couldn't get SingularityTaskId for {}", taskUsage);
          continue;
        }

        SingularityTaskUsage latestUsage = getUsage(taskUsage);
        List<SingularityTaskUsage> pastTaskUsages = usageManager.getTaskUsage(taskId);


        clearOldUsage(taskId);
        usageManager.saveSpecificTaskUsage(taskId, latestUsage);

        Optional<SingularityTask> maybeTask = taskManager.getTask(task);
        Optional<Resources> maybeResources = Optional.absent();
        if (maybeTask.isPresent()) {
          maybeResources = maybeTask.get().getTaskRequest().getPendingTask().getResources().or(maybeTask.get().getTaskRequest().getDeploy().getResources());
          if (maybeResources.isPresent()) {
            Resources taskResources = maybeResources.get();
            double memoryMbReservedForTask = taskResources.getMemoryMb();
            double cpuReservedForTask = taskResources.getCpus();
            double diskMbReservedForTask = taskResources.getDiskMb();

            memoryMbReservedOnSlave += memoryMbReservedForTask;
            cpuReservedOnSlave += cpuReservedForTask;
            diskMbReservedOnSlave += diskMbReservedForTask;

            runWithRequestLock(() -> updateRequestUtilization(utilizationPerRequestId, previousUtilizations.get(maybeTask.get().getTaskRequest().getRequest().getId()), pastTaskUsages, latestUsage, task, memoryMbReservedForTask, cpuReservedForTask, diskMbReservedForTask), task.getRequestId());
          }
        }
        memoryBytesUsedOnSlave += latestUsage.getMemoryTotalBytes();
        diskMbUsedOnSlave += latestUsage.getDiskTotalBytes();

        SingularityTaskCurrentUsage currentUsage = null;
        if (pastTaskUsages.isEmpty()) {
          Optional<SingularityTaskHistoryUpdate> maybeStartingUpdate = taskManager.getTaskHistoryUpdate(task, ExtendedTaskState.TASK_STARTING);
          if (maybeStartingUpdate.isPresent()) {
            long startTimestampSeconds = TimeUnit.MILLISECONDS.toSeconds(maybeStartingUpdate.get().getTimestamp());
            double usedCpusSinceStart = latestUsage.getCpuSeconds() / (latestUsage.getTimestamp() - startTimestampSeconds);
            currentUsage = new SingularityTaskCurrentUsage(latestUsage.getMemoryTotalBytes(), now, usedCpusSinceStart, latestUsage.getDiskTotalBytes());
            usageManager.saveCurrentTaskUsage(taskId, currentUsage);

            cpusUsedOnSlave += usedCpusSinceStart;
          }
        } else {
          SingularityTaskUsage lastUsage = pastTaskUsages.get(pastTaskUsages.size() - 1);

          double taskCpusUsed = ((latestUsage.getCpuSeconds() - lastUsage.getCpuSeconds()) / (latestUsage.getTimestamp() - lastUsage.getTimestamp()));

          currentUsage = new SingularityTaskCurrentUsage(latestUsage.getMemoryTotalBytes(), now, taskCpusUsed, latestUsage.getDiskTotalBytes());
          usageManager.saveCurrentTaskUsage(taskId, currentUsage);
          cpusUsedOnSlave += taskCpusUsed;
        }

        if (configuration.isShuffleTasksForOverloadedSlaves() && currentUsage != null && currentUsage.getCpusUsed() > 0) {
          if (isEligibleForShuffle(task)) {
            Optional<SingularityTaskHistoryUpdate> maybeCleanupUpdate = taskManager.getTaskHistoryUpdate(task, ExtendedTaskState.TASK_CLEANING);
            if (maybeCleanupUpdate.isPresent() && isTaskAlreadyCleanedUpForShuffle(maybeCleanupUpdate.get())) {
              LOG.trace("Task {} already being cleaned up to spread cpu usage, skipping", taskId);
            } else {
              if (maybeResources.isPresent()) {
                possibleTasksToShuffle.add(new TaskIdWithUsage(task, maybeResources.get(), currentUsage));
              }
            }
          }
        }
      }

      if (!slave.getResources().isPresent() ||
          !slave.getResources().get().getMemoryMegaBytes().isPresent() ||
          !slave.getResources().get().getNumCpus().isPresent()) {
        LOG.debug("Could not find slave or resources for slave {}", slave.getId());
      } else {
        memoryMbTotal = Optional.of(slave.getResources().get().getMemoryMegaBytes().get().longValue());
        cpusTotal = Optional.of(slave.getResources().get().getNumCpus().get().doubleValue());
        diskMbTotal = Optional.of(slave.getResources().get().getDiskSpace().get());
      }

      SingularitySlaveUsage slaveUsage = new SingularitySlaveUsage(cpusUsedOnSlave, cpuReservedOnSlave, cpusTotal, memoryBytesUsedOnSlave, memoryMbReservedOnSlave,
          memoryMbTotal, diskMbUsedOnSlave, diskMbReservedOnSlave, diskMbTotal, allTaskUsage.size(), now,
          systemMemTotalBytes, systemMemFreeBytes, systemCpusTotal, systemLoad1Min, systemLoad5Min, systemLoad15Min, slaveDiskUsed, slaveDiskTotal);

      if (slaveOverloaded) {
        overLoadedHosts.put(slaveUsage, possibleTasksToShuffle);
      }

      List<Long> slaveTimestamps = usageManager.getSlaveUsageTimestamps(slave.getId());
      if (slaveTimestamps.size() + 1 > configuration.getNumUsageToKeep()) {
        usageManager.deleteSpecificSlaveUsage(slave.getId(), slaveTimestamps.get(0));
      }

      if (slaveUsage.getMemoryBytesTotal().isPresent() && slaveUsage.getCpusTotal().isPresent()) {
        totalMemBytesUsed.getAndAdd((long) slaveUsage.getMemoryBytesUsed());
        totalCpuUsed.getAndAdd(slaveUsage.getCpusUsed());
        totalDiskBytesUsed.getAndAdd((long) slaveUsage.getDiskBytesUsed());

        totalMemBytesAvailable.getAndAdd(slaveUsage.getMemoryBytesTotal().get());
        totalCpuAvailable.getAndAdd(slaveUsage.getCpusTotal().get());
        totalDiskBytesAvailable.getAndAdd(slaveUsage.getDiskBytesTotal().get());
      }

      LOG.debug("Saving slave {} usage {}", slave.getHost(), slaveUsage);
      usageManager.saveSpecificSlaveUsageAndSetCurrent(slave.getId(), slaveUsage);
    } catch (Throwable t) {
      String message = String.format("Could not get slave usage for host %s", slave.getHost());
      LOG.error(message, t);
      exceptionNotifier.notify(message, t);
    }
  }

  private SingularityTaskUsage getUsage(MesosTaskMonitorObject taskUsage) {
    return new SingularityTaskUsage(
        taskUsage.getStatistics().getMemTotalBytes(),
        taskUsage.getStatistics().getTimestamp(),
        taskUsage.getStatistics().getCpusSystemTimeSecs() + taskUsage.getStatistics().getCpusUserTimeSecs(),
        taskUsage.getStatistics().getDiskUsedBytes(),
        taskUsage.getStatistics().getCpusNrPeriods(),
        taskUsage.getStatistics().getCpusNrThrottled(),
        taskUsage.getStatistics().getCpusThrottledTimeSecs());
  }

  private List<SingularityTaskUsage> getFullListOfTaskUsages(List<SingularityTaskUsage> pastTaskUsages, SingularityTaskUsage latestUsage, SingularityTaskId task) {
    List<SingularityTaskUsage> pastTaskUsagesCopy = new ArrayList<>();
    pastTaskUsagesCopy.add(new SingularityTaskUsage(0, TimeUnit.MILLISECONDS.toSeconds(task.getStartedAt()), 0, 0, 0 , 0, 0)); // to calculate oldest cpu usage
    pastTaskUsagesCopy.addAll(pastTaskUsages);
    pastTaskUsagesCopy.add(latestUsage);

    return pastTaskUsagesCopy;
  }


  private boolean isEligibleForShuffle(SingularityTaskId task) {
    Optional<SingularityTaskHistoryUpdate> taskRunning = taskManager.getTaskHistoryUpdate(task, ExtendedTaskState.TASK_RUNNING);

    return (
        !configuration.getDoNotShuffleRequests().contains(task.getRequestId())
            && isLongRunning(task)
            && (
            configuration.getMinutesBeforeNewTaskEligibleForShuffle() == 0 // Shuffle delay is disabled entirely
                || (taskRunning.isPresent() && TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - taskRunning.get()
                .getTimestamp()) >= configuration.getMinutesBeforeNewTaskEligibleForShuffle())
        )
    );
  }

  private boolean isLongRunning(SingularityTaskId task) {
    Optional<SingularityRequestWithState> request = requestManager.getRequest(task.getRequestId());
    if (request.isPresent()) {
      return request.get().getRequest().getRequestType().isLongRunning();
    }

    LOG.warn("Couldn't find request id {} for task {}", task.getRequestId(), task.getId());
    return false;
  }

  private boolean isTaskAlreadyCleanedUpForShuffle(SingularityTaskHistoryUpdate taskHistoryUpdate) {
    if (taskHistoryUpdate.getStatusMessage().or("").contains(TaskCleanupType.REBALANCE_CPU_USAGE.name())) {
      return true;
    }
    for (SingularityTaskHistoryUpdate previous : taskHistoryUpdate.getPrevious()) {
      if (previous.getStatusMessage().or("").contains(TaskCleanupType.REBALANCE_CPU_USAGE.name())) {
        return true;
      }
    }
    return false;
  }

  private void updateRequestUtilization(Map<String, RequestUtilization> utilizationPerRequestId,
      RequestUtilization previous,
      List<SingularityTaskUsage> pastTaskUsages,
      SingularityTaskUsage latestUsage,
      SingularityTaskId task,
      double memoryMbReservedForTask,
      double cpuReservedForTask,
      double diskMbReservedForTask) {
    String requestId = task.getRequestId();
    RequestUtilization newRequestUtilization = utilizationPerRequestId.getOrDefault(requestId, new RequestUtilization(requestId, task.getDeployId()));
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
        newRequestUtilization.setMaxPercentCpuTimeThrottled(previous.getMaxPercentCpuTimeThrottled());
        newRequestUtilization.setMaxCpuThrottledTimestamp(previous.getMaxCpuThrottledTimestamp());
      }
      if (previous.getMinCpuThrottledTimestamp() < DAY_IN_SECONDS) {
        newRequestUtilization.setMinPercentCpuTimeThrottled(previous.getMinPercentCpuTimeThrottled());
        newRequestUtilization.setMinCpuThrottledTimestamp(previous.getMinCpuThrottledTimestamp());
      }
    }

    List<SingularityTaskUsage> pastTaskUsagesCopy = getFullListOfTaskUsages(pastTaskUsages, latestUsage, task);
    pastTaskUsagesCopy.sort(Comparator.comparingDouble(SingularityTaskUsage::getTimestamp));
    int numTasks = pastTaskUsagesCopy.size() - 1; // One usage is a fake 0 usage to calculate first cpu times

    int numCpuOverages = 0;

    for (int i = 0; i < numTasks; i++) {
      SingularityTaskUsage olderUsage = pastTaskUsagesCopy.get(i);
      SingularityTaskUsage newerUsage = pastTaskUsagesCopy.get(i + 1);
      double cpusUsed = (newerUsage.getCpuSeconds() - olderUsage.getCpuSeconds()) / (newerUsage.getTimestamp() - olderUsage.getTimestamp());
      double percentCpuTimeThrottled = (newerUsage.getCpusThrottledTimeSecs() - olderUsage.getCpusThrottledTimeSecs()) / (newerUsage.getTimestamp() - olderUsage.getTimestamp());

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
      if (percentCpuTimeThrottled > newRequestUtilization.getMaxPercentCpuTimeThrottled()) {
        newRequestUtilization.setMaxPercentCpuTimeThrottled(percentCpuTimeThrottled);
        newRequestUtilization.setMaxCpuThrottledTimestamp(newerUsage.getTimestamp());
      }
      if (percentCpuTimeThrottled < newRequestUtilization.getMinPercentCpuTimeThrottled()) {
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

    double cpuBurstRating = pastTaskUsagesCopy.size() > 0 ? numCpuOverages / (double) pastTaskUsagesCopy.size() : 1;

    newRequestUtilization
        .addMemBytesReserved((long) (memoryMbReservedForTask * SingularitySlaveUsage.BYTES_PER_MEGABYTE * numTasks))
        .addCpuReserved(cpuReservedForTask * numTasks)
        .addDiskBytesReserved((long) diskMbReservedForTask * SingularitySlaveUsage.BYTES_PER_MEGABYTE * numTasks)
        .setCpuBurstRating(cpuBurstRating);

    utilizationPerRequestId.put(requestId, newRequestUtilization);
  }

  @VisibleForTesting
  void clearOldUsage(String taskId) {
    usageManager.getTaskUsagePaths(taskId)
        .stream()
        .map(Double::parseDouble)
        .skip(configuration.getNumUsageToKeep())
        .forEach((pathId) -> {
          SingularityDeleteResult result = usageManager.deleteSpecificTaskUsage(taskId, pathId);
          if (result.equals(SingularityDeleteResult.DIDNT_EXIST)) {
            LOG.warn("Didn't delete taskUsage {} for taskId {}", pathId.toString(), taskId);
          }
        });
  }

  public void runWithRequestLock(Runnable function, String requestId) {
    ReentrantLock lock = requestLocks.computeIfAbsent(requestId, (r) -> new ReentrantLock());
    lock.lock();
    try {
      function.run();
    } finally {
      lock.unlock();
    }
  }
}
