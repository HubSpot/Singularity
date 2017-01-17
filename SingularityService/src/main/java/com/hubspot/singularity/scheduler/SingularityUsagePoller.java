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
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.UsageManager;

public class SingularityUsagePoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityTaskReconciliationPoller.class);

  private final SingularityConfiguration configuration;
  private final MesosClient mesosClient;
  private final UsageManager usageManager;
  private final SingularityUsageHelper usageHelper;

  @Inject
  SingularityUsagePoller(SingularityConfiguration configuration, SingularityUsageHelper usageHelper, UsageManager usageManager, MesosClient mesosClient) {
    super(configuration.getCheckUsageEveryMillis(), TimeUnit.MILLISECONDS);

    this.configuration = configuration;
    this.usageHelper = usageHelper;
    this.mesosClient = mesosClient;
    this.usageManager = usageManager;
  }

  @Override
  public void runActionOnPoll() {
    for (SingularitySlave slave : usageHelper.getSlavesToTrackUsageFor()) {
      long memoryBytesUsed = 0;
      double cpusUsed = 0;

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

          cpusUsed += ((usage.getCpuSeconds() - lastUsage.getCpuSeconds()) / (usage.getTimestamp() - lastUsage.getTimestamp()));
        }
      }

      SingularitySlaveUsage slaveUsage = new SingularitySlaveUsage(memoryBytesUsed, System.currentTimeMillis(), cpusUsed, allTaskUsage.size());
      List<Long> slaveTimestamps = usageManager.getSlaveUsageTimestamps(slave.getId());
      if (slaveTimestamps.size() + 1 > configuration.getNumUsageToKeep()) {
        usageManager.deleteSpecificSlaveUsage(slave.getId(), slaveTimestamps.get(0));
      }

      LOG.debug("Saving slave usage {}", slaveUsage);
      usageManager.saveSpecificSlaveUsage(slave.getId(), slaveUsage);
    }
  }

  private SingularityTaskUsage getUsage(MesosTaskMonitorObject taskUsage) {
    double cpuSeconds = taskUsage.getStatistics().getCpusSystemTimeSecs() + taskUsage.getStatistics().getCpusUserTimeSecs();

    return new SingularityTaskUsage(taskUsage.getStatistics().getMemRssBytes(), taskUsage.getStatistics().getTimestamp(), cpuSeconds);
  }

}
