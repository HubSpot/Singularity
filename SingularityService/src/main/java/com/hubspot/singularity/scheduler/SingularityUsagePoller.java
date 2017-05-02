package com.hubspot.singularity.scheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.common.base.Optional;
import com.hubspot.mesos.client.MesosClient;
import com.hubspot.mesos.json.MesosTaskMonitorObject;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularitySlaveUsage.ResourceUsageType;
import com.hubspot.singularity.SingularityTaskCurrentUsage;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
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

  @Inject
  SingularityUsagePoller(SingularityConfiguration configuration,
                         SingularityUsageHelper usageHelper,
                         UsageManager usageManager,
                         MesosClient mesosClient,
                         SingularityExceptionNotifier exceptionNotifier,
                         RequestManager requestManager,
                         DeployManager deployManager) {
    super(configuration.getCheckUsageEveryMillis(), TimeUnit.MILLISECONDS);

    this.configuration = configuration;
    this.usageHelper = usageHelper;
    this.mesosClient = mesosClient;
    this.usageManager = usageManager;
    this.exceptionNotifier = exceptionNotifier;
    this.requestManager = requestManager;
    this.deployManager = deployManager;
  }

  @Override
  public void runActionOnPoll() {
    final long now = System.currentTimeMillis();

    Map<ResourceUsageType, Number> longRunningTasksUsage = new HashMap<>();
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);

    for (SingularitySlave slave : usageHelper.getSlavesToTrackUsageFor()) {
      Optional<Long> memoryMbTotal = Optional.absent();
      Optional<Double> cpuTotal = Optional.absent();
      long memoryBytesUsed = 0;
      double cpusUsed = 0;

      try {
        List<MesosTaskMonitorObject> allTaskUsage = mesosClient.getSlaveResourceUsage(slave.getHost());

        for (MesosTaskMonitorObject taskUsage : allTaskUsage) {
          String taskId = taskUsage.getSource();

          SingularityTaskUsage usage = getUsage(taskUsage);

          List<SingularityTaskUsage> taskUsages = usageManager.getTaskUsage(taskId);

          if (taskUsages.size() + 1 > configuration.getNumUsageToKeep()) {
            usageManager.deleteSpecificTaskUsage(taskId, taskUsages.get(0).getTimestamp());
          }
          usageManager.saveSpecificTaskUsage(taskId, usage);

          memoryBytesUsed += usage.getMemoryRssBytes();

          if (!taskUsages.isEmpty()) {
            SingularityTaskUsage lastUsage = taskUsages.get(taskUsages.size() - 1);

            double taskCpusUsed = ((usage.getCpuSeconds() - lastUsage.getCpuSeconds()) / (usage.getTimestamp() - lastUsage.getTimestamp()));

            if (getRequestType(taskUsage).isLongRunning() ||  isConsideredLongRunning(taskUsage)) {
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
          cpuTotal = Optional.of(slave.getResources().get().getNumCpus().get().doubleValue());
        }

        SingularitySlaveUsage slaveUsage = new SingularitySlaveUsage(memoryBytesUsed, now, cpusUsed, allTaskUsage.size(), memoryMbTotal, cpuTotal, longRunningTasksUsage);
        List<Long> slaveTimestamps = usageManager.getSlaveUsageTimestamps(slave.getId());
        if (slaveTimestamps.size() + 1 > configuration.getNumUsageToKeep()) {
          usageManager.deleteSpecificSlaveUsage(slave.getId(), slaveTimestamps.get(0));
        }

        LOG.debug("Saving slave usage {}", slaveUsage);
        usageManager.saveSpecificSlaveUsageAndSetCurrent(slave.getId(), slaveUsage);
      } catch (Exception e) {
        String message = String.format("Could not get slave usage for host %s", slave.getHost());
        LOG.error(message, e);
        exceptionNotifier.notify(message, e);
      }
    }
  }

  private SingularityTaskUsage getUsage(MesosTaskMonitorObject taskUsage) {
    double cpuSeconds = taskUsage.getStatistics().getCpusSystemTimeSecs() + taskUsage.getStatistics().getCpusUserTimeSecs();

    return new SingularityTaskUsage(taskUsage.getStatistics().getMemRssBytes(), taskUsage.getStatistics().getTimestamp(), cpuSeconds);
  }

  private RequestType getRequestType(MesosTaskMonitorObject task) {
    return requestManager.getRequest(SingularityTaskId.valueOf(task.getSource()).getRequestId()).get().getRequest().getRequestType();
  }

  private boolean isConsideredLongRunning(MesosTaskMonitorObject task) {
    SingularityTaskId taskId = SingularityTaskId.valueOf(task.getSource());
    final Optional<SingularityDeployStatistics> deployStatistics = deployManager.getDeployStatistics(taskId.getRequestId(), taskId.getDeployId());

    if (deployStatistics.isPresent() && deployStatistics.get().getAverageRuntimeMillis().isPresent()) {
      return deployStatistics.get().getAverageRuntimeMillis().get() >= configuration.getConsiderNonLongRunningTaskLongRunningAfterRunningForSeconds();
    }

    return false;
  }

  private void updateLongRunningTasksUsage(Map<ResourceUsageType, Number> longRunningTasksUsage, long memBytesUsed, double cpuUsed) {
    longRunningTasksUsage.compute(ResourceUsageType.MEMORY_BYTES_USED, (k, v) -> (v == null) ? memBytesUsed : v.longValue() + memBytesUsed);
    longRunningTasksUsage.compute(ResourceUsageType.CPU_USED, (k, v) -> (v == null) ? cpuUsed : v.doubleValue() + cpuUsed);
  }
}
