package com.hubspot.singularity.scheduler;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.mesos.client.MesosClient;
import com.hubspot.mesos.json.MesosTaskMonitorObject;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularityTaskCurrentUsage;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.UsageManager;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

public class SingularityUsagePoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityTaskReconciliationPoller.class);

  private final SingularityConfiguration configuration;
  private final MesosClient mesosClient;
  private final UsageManager usageManager;
  private final SingularityUsageHelper usageHelper;
  private final SingularityExceptionNotifier exceptionNotifier;

  @Inject
  SingularityUsagePoller(SingularityConfiguration configuration, SingularityUsageHelper usageHelper, UsageManager usageManager, MesosClient mesosClient, SingularityExceptionNotifier exceptionNotifier) {
    super(configuration.getCheckUsageEveryMillis(), TimeUnit.MILLISECONDS);

    this.configuration = configuration;
    this.usageHelper = usageHelper;
    this.mesosClient = mesosClient;
    this.usageManager = usageManager;
    this.exceptionNotifier = exceptionNotifier;
  }

  @Override
  public void runActionOnPoll() {
    final long now = System.currentTimeMillis();

    for (SingularitySlave slave : usageHelper.getSlavesToTrackUsageFor()) {
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

            SingularityTaskCurrentUsage currentUsage = new SingularityTaskCurrentUsage(usage.getMemoryRssBytes(), now, taskCpusUsed);

            usageManager.saveCurrentTaskUsage(taskId, currentUsage);

            cpusUsed += taskCpusUsed;
          }
        }

        SingularitySlaveUsage slaveUsage = new SingularitySlaveUsage(memoryBytesUsed, now, cpusUsed, allTaskUsage.size());
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

}
