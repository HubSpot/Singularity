package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.mesos.client.MesosClient;
import com.hubspot.mesos.json.MesosTaskMonitorObject;
import com.hubspot.singularity.InvalidSingularityTaskIdException;
import com.hubspot.singularity.RequestUtilization;
import com.hubspot.singularity.SingularityClusterUtilization;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularitySlaveUsage.ResourceUsageType;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCurrentUsage;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.UsageManager;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

public class SingularityUsagePoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityUsagePoller.class);

  private final SingularityConfiguration configuration;
  private final MesosClient mesosClient;
  private final UsageManager usageManager;
  private final SingularityUsageHelper usageHelper;
  private final SingularityExceptionNotifier exceptionNotifier;
  private final RequestManager requestManager;
  private final DeployManager deployManager;
  private final TaskManager taskManager;

  @Inject
  SingularityUsagePoller(SingularityConfiguration configuration,
                         SingularityUsageHelper usageHelper,
                         UsageManager usageManager,
                         MesosClient mesosClient,
                         SingularityExceptionNotifier exceptionNotifier,
                         RequestManager requestManager,
                         DeployManager deployManager,
                         TaskManager taskManager) {
    super(configuration.getCheckUsageEveryMillis(), TimeUnit.MILLISECONDS);

    this.configuration = configuration;
    this.usageHelper = usageHelper;
    this.mesosClient = mesosClient;
    this.usageManager = usageManager;
    this.exceptionNotifier = exceptionNotifier;
    this.requestManager = requestManager;
    this.deployManager = deployManager;
    this.taskManager = taskManager;
  }

  @Override
  public void runActionOnPoll() {
    Map<String, RequestUtilization> utilizationPerRequestId = new HashMap<>();
    final long now = System.currentTimeMillis();

    long totalMemBytesUsed = 0;
    long totalMemBytesAvailable = 0;
    double totalCpuUsed = 0.00;
    double totalCpuAvailable = 0.00;

    for (SingularitySlave slave : usageHelper.getSlavesToTrackUsageFor()) {
      Map<ResourceUsageType, Number> longRunningTasksUsage = new HashMap<>();
      longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
      longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
      Optional<Long> memoryMbTotal = Optional.absent();
      Optional<Double> cpusTotal = Optional.absent();
      long memoryMbReserved = 0;
      long cpuReserved = 0;
      long memoryBytesUsed = 0;
      double cpusUsed = 0;

      try {
        List<MesosTaskMonitorObject> allTaskUsage = mesosClient.getSlaveResourceUsage(slave.getHost());

        for (MesosTaskMonitorObject taskUsage : allTaskUsage) {
          String taskId = taskUsage.getSource();
          SingularityTaskId task;
          try {
            task = SingularityTaskId.valueOf(taskId);
          } catch (InvalidSingularityTaskIdException e) {
            LOG.error("Couldn't get SingularityTaskId for {}", taskUsage);
            continue;
          }

          RequestUtilization requestUtilization = utilizationPerRequestId.getOrDefault(task.getRequestId(), new RequestUtilization(task.getRequestId(), task.getDeployId()));

          SingularityTaskUsage usage = getUsage(taskUsage);

          List<SingularityTaskUsage> taskUsages = usageManager.getTaskUsage(taskId);

          taskUsages.forEach(tu -> {
            requestUtilization.addCpu(tu.getCpuSeconds());
            requestUtilization.addMemBytes(tu.getMemoryRssBytes());
            requestUtilization.incrementTaskCount();
          });

          utilizationPerRequestId.put(task.getRequestId(), requestUtilization);

          clearOldUsage(taskUsages, taskId);

          usageManager.saveSpecificTaskUsage(taskId, usage);

          Optional<SingularityTask> maybeTask = taskManager.getTask(task);
          if (maybeTask.isPresent() && maybeTask.get().getTaskRequest().getDeploy().getResources().isPresent()) {
            memoryMbReserved += maybeTask.get().getTaskRequest().getDeploy().getResources().get().getMemoryMb();
            cpuReserved += maybeTask.get().getTaskRequest().getDeploy().getResources().get().getCpus();
          }
          memoryBytesUsed += usage.getMemoryRssBytes();

          if (!taskUsages.isEmpty()) {
            SingularityTaskUsage lastUsage = taskUsages.get(taskUsages.size() - 1);

            double taskCpusUsed = ((usage.getCpuSeconds() - lastUsage.getCpuSeconds()) / (usage.getTimestamp() - lastUsage.getTimestamp()));

            if (isLongRunning(task) ||  isConsideredLongRunning(task)) {
              updateLongRunningTasksUsage(longRunningTasksUsage, usage.getMemoryRssBytes(), taskCpusUsed);
            }
            SingularityTaskCurrentUsage currentUsage = new SingularityTaskCurrentUsage(usage.getMemoryRssBytes(), now, taskCpusUsed);

            usageManager.saveCurrentTaskUsage(taskId, currentUsage);

            cpusUsed += taskCpusUsed;
          }
        }

        if (!slave.getResources().isPresent() ||
            !slave.getResources().get().getMemoryMegaBytes().isPresent() ||
            !slave.getResources().get().getNumCpus().isPresent()) {
          LOG.debug("Could not find slave or resources for slave {}", slave.getId());
        } else {
          memoryMbTotal = Optional.of(slave.getResources().get().getMemoryMegaBytes().get().longValue());
          cpusTotal = Optional.of(slave.getResources().get().getNumCpus().get().doubleValue());
        }

        SingularitySlaveUsage slaveUsage = new SingularitySlaveUsage(memoryBytesUsed, memoryMbReserved, now, cpusUsed, cpuReserved, allTaskUsage.size(), memoryMbTotal, cpusTotal, longRunningTasksUsage);
        List<Long> slaveTimestamps = usageManager.getSlaveUsageTimestamps(slave.getId());
        if (slaveTimestamps.size() + 1 > configuration.getNumUsageToKeep()) {
          usageManager.deleteSpecificSlaveUsage(slave.getId(), slaveTimestamps.get(0));
        }

        if (slaveUsage.getMemoryBytesTotal().isPresent() && slaveUsage.getCpusTotal().isPresent()) {
          totalMemBytesUsed += slaveUsage.getMemoryBytesUsed();
          totalCpuUsed += slaveUsage.getCpusUsed();

          totalMemBytesAvailable += slaveUsage.getMemoryBytesTotal().get();
          totalCpuAvailable += slaveUsage.getCpusTotal().get();
        }

        LOG.debug("Saving slave {} usage {}", slave.getHost(), slaveUsage);
        usageManager.saveSpecificSlaveUsageAndSetCurrent(slave.getId(), slaveUsage);
      } catch (Exception e) {
        String message = String.format("Could not get slave usage for host %s", slave.getHost());
        LOG.error(message, e);
        exceptionNotifier.notify(message, e);
      }
    }
    usageManager.saveClusterUtilization(getClusterUtilization(utilizationPerRequestId, configuration.getMinUnderUtilizedPct(), totalMemBytesUsed, totalMemBytesAvailable, totalCpuUsed, totalCpuAvailable, now));
  }

  private SingularityTaskUsage getUsage(MesosTaskMonitorObject taskUsage) {
    double cpuSeconds = taskUsage.getStatistics().getCpusSystemTimeSecs() + taskUsage.getStatistics().getCpusUserTimeSecs();

    return new SingularityTaskUsage(taskUsage.getStatistics().getMemTotalBytes(), taskUsage.getStatistics().getTimestamp(), cpuSeconds);
  }

  private boolean isLongRunning(SingularityTaskId task) {
    Optional<SingularityRequestWithState> request = requestManager.getRequest(task.getRequestId());
    if (request.isPresent()) {
      return request.get().getRequest().getRequestType().isLongRunning();
    }

    LOG.warn("Couldn't find request id {} for task {}", task.getRequestId(), task.getId());
    return false;
  }

  private boolean isConsideredLongRunning(SingularityTaskId task) {
    final Optional<SingularityDeployStatistics> deployStatistics = deployManager.getDeployStatistics(task.getRequestId(), task.getDeployId());

    return deployStatistics.isPresent() && deployStatistics.get().getAverageRuntimeMillis().isPresent() &&
        deployStatistics.get().getAverageRuntimeMillis().get() >= configuration.getConsiderNonLongRunningTaskLongRunningAfterRunningForSeconds();
  }

  private void updateLongRunningTasksUsage(Map<ResourceUsageType, Number> longRunningTasksUsage, long memBytesUsed, double cpuUsed) {
    longRunningTasksUsage.compute(ResourceUsageType.MEMORY_BYTES_USED, (k, v) -> (v == null) ? memBytesUsed : v.longValue() + memBytesUsed);
    longRunningTasksUsage.compute(ResourceUsageType.CPU_USED, (k, v) -> (v == null) ? cpuUsed : v.doubleValue() + cpuUsed);
  }

  private SingularityClusterUtilization getClusterUtilization(Map<String, RequestUtilization> utilizationPerRequestId, double minUnderUtilizedPct, long totalMemBytesUsed, long totalMemBytesAvailable, double totalCpuUsed, double totalCpuAvailable, long now) {
    int numRequestsWithUnderUtilizedCpu = 0;
    int numRequestsWithOverUtilizedCpu = 0;
    int numRequestsWithUnderUtilizedMemBytes = 0;

    double totalUnderUtilizedCpu = 0;
    double totalOverUtilizedCpu = 0;
    long totalUnderUtilizedMemBytes = 0;

    double maxUnderUtilizedCpu = Double.MIN_VALUE;
    double maxOverUtilizedCpu = Double.MIN_VALUE;
    long maxUnderUtilizedMemBytes = Long.MIN_VALUE;

    double minUnderUtilizedCpu = Double.MAX_VALUE;
    double minOverUtilizedCpu = Double.MAX_VALUE;
    long minUnderUtilizedMemBytes = Long.MAX_VALUE;


    for (Iterator<Entry<String, RequestUtilization>> it = utilizationPerRequestId.entrySet().iterator(); it.hasNext(); ) {
      RequestUtilization utilization = it.next().getValue();
      Optional<SingularityDeploy> maybeDeploy = deployManager.getDeploy(utilization.getRequestId(), utilization.getDeployId());

      if (maybeDeploy.isPresent() && maybeDeploy.get().getResources().isPresent()) {
        boolean includeUtilization = true;
        long memoryBytesReserved = (long) (maybeDeploy.get().getResources().get().getMemoryMb() * SingularitySlaveUsage.BYTES_PER_MEGABYTE);
        double cpuReserved = maybeDeploy.get().getResources().get().getCpus();

        double unusedCpu = cpuReserved - utilization.getAvgCpuUsed();
        long unusedMemBytes = memoryBytesReserved - utilization.getMemBytesTotal();

        if (unusedCpu / cpuReserved >= minUnderUtilizedPct) {
          numRequestsWithUnderUtilizedCpu++;
          totalUnderUtilizedCpu += unusedCpu;
          maxUnderUtilizedCpu = Math.max(unusedCpu, maxUnderUtilizedCpu);
          minUnderUtilizedCpu = Math.min(unusedCpu, minUnderUtilizedCpu);
        } else if (unusedCpu < 0) {
          double overusedCpu = Math.abs(unusedCpu);

          numRequestsWithOverUtilizedCpu++;
          totalOverUtilizedCpu += overusedCpu;
          maxOverUtilizedCpu = Math.max(overusedCpu, maxOverUtilizedCpu);
          minOverUtilizedCpu = Math.min(overusedCpu, minOverUtilizedCpu);
        } else {
          includeUtilization = false;
        }

        if (unusedMemBytes / memoryBytesReserved >= minUnderUtilizedPct) {
          numRequestsWithUnderUtilizedMemBytes++;
          totalUnderUtilizedMemBytes += unusedMemBytes;
          maxUnderUtilizedMemBytes = Math.max(unusedMemBytes, maxUnderUtilizedMemBytes);
          minUnderUtilizedMemBytes = Math.min(unusedMemBytes, minUnderUtilizedMemBytes);
        } else if (!includeUtilization) {
          it.remove();
        }
      }
    }

    double avgUnderUtilizedCpu = totalUnderUtilizedCpu / numRequestsWithUnderUtilizedCpu;
    double avgOverUtilizedCpu = totalOverUtilizedCpu / numRequestsWithOverUtilizedCpu;
    double avgUnderUtilizedMemBytes = totalUnderUtilizedMemBytes / numRequestsWithUnderUtilizedMemBytes;

    return new SingularityClusterUtilization(new ArrayList<>(utilizationPerRequestId.values()), numRequestsWithUnderUtilizedCpu, numRequestsWithOverUtilizedCpu,
        numRequestsWithUnderUtilizedMemBytes, totalUnderUtilizedCpu, totalOverUtilizedCpu, totalUnderUtilizedMemBytes, avgUnderUtilizedCpu,
        avgOverUtilizedCpu, avgUnderUtilizedMemBytes, maxUnderUtilizedCpu, maxOverUtilizedCpu, maxUnderUtilizedMemBytes, minUnderUtilizedCpu,
        minOverUtilizedCpu, minUnderUtilizedMemBytes, totalMemBytesUsed, totalMemBytesAvailable, totalCpuUsed, totalCpuAvailable, now);
  }


  @VisibleForTesting
  void clearOldUsage(List<SingularityTaskUsage> taskUsages, String taskId) {
    if (taskUsages.size() + 1 > configuration.getNumUsageToKeep()) {
      long minMillisApart = configuration.getUsageIntervalMultiplier() * configuration.getCheckUsageEveryMillis();

      for (int i = 0; i < taskUsages.size() - 1; i++) {
        if (taskUsages.get(i + 1).getTimestamp() - taskUsages.get(i).getTimestamp() < minMillisApart) {
          usageManager.deleteSpecificTaskUsage(taskId, taskUsages.get(i + 1).getTimestamp());
          return;
        }
      }

      usageManager.deleteSpecificTaskUsage(taskId, taskUsages.get(0).getTimestamp());
    }
  }
}
