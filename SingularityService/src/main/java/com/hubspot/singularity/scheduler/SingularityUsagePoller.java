package com.hubspot.singularity.scheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.hubspot.mesos.client.MesosClient;
import com.hubspot.mesos.json.MesosTaskMonitorObject;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularityTaskCurrentUsage;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.UsageManager;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

public class SingularityUsagePoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityTaskReconciliationPoller.class);

  private final SingularityConfiguration configuration;
  private final MesosClient mesosClient;
  private final UsageManager usageManager;
  private final SingularityUsageHelper usageHelper;
  private final SingularityExceptionNotifier exceptionNotifier;
  private final RequestManager requestManager;

  @Inject
  SingularityUsagePoller(SingularityConfiguration configuration,
                         SingularityUsageHelper usageHelper,
                         UsageManager usageManager,
                         MesosClient mesosClient,
                         SingularityExceptionNotifier exceptionNotifier,
                         RequestManager requestManager) {
    super(configuration.getCheckUsageEveryMillis(), TimeUnit.MILLISECONDS);

    this.configuration = configuration;
    this.usageHelper = usageHelper;
    this.mesosClient = mesosClient;
    this.usageManager = usageManager;
    this.exceptionNotifier = exceptionNotifier;
    this.requestManager = requestManager;
  }

  @Override
  public void runActionOnPoll() {
    final long now = System.currentTimeMillis();
    Map<RequestType, Map<String, Number>> usagesPerRequestType = new HashMap<>();

    for (SingularitySlave slave : usageHelper.getSlavesToTrackUsageFor()) {
      Optional<Long> memoryMbTotal = Optional.empty();
      Optional<Double> cpuTotal = Optional.empty();
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

            updateUsagesPerRequestType(usagesPerRequestType, getRequestType(taskUsage), usage.getMemoryRssBytes(), taskCpusUsed);
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

        SingularitySlaveUsage slaveUsage = new SingularitySlaveUsage(memoryBytesUsed, now, cpusUsed, allTaskUsage.size(), memoryMbTotal, cpuTotal, usagesPerRequestType);
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

  private void updateUsagesPerRequestType(Map<RequestType, Map<String, Number>> usagePerRequestType, RequestType type, long memBytesUsed, double cpuUsed) {
    if (usagePerRequestType.containsKey(type)) {
      long oldMemUsed = 0L;
      double oldCpuUsed = 0;

      if (usagePerRequestType.get(type).containsKey(SingularitySlaveUsage.MEMORY_BYTES_USED)) {
         oldMemUsed = usagePerRequestType.get(type).get(SingularitySlaveUsage.MEMORY_BYTES_USED).longValue();
      }
      if (usagePerRequestType.get(type).containsKey(SingularitySlaveUsage.CPU_USED)) {
        oldCpuUsed = usagePerRequestType.get(type).get(SingularitySlaveUsage.CPU_USED).doubleValue();
      }

      usagePerRequestType.get(type).put(SingularitySlaveUsage.MEMORY_BYTES_USED, oldMemUsed + memBytesUsed);
      usagePerRequestType.get(type).put(SingularitySlaveUsage.CPU_USED, oldCpuUsed + cpuUsed);
    } else {
      usagePerRequestType.put(type, ImmutableMap.of(SingularitySlaveUsage.MEMORY_BYTES_USED, memBytesUsed));
      usagePerRequestType.put(type, ImmutableMap.of(SingularitySlaveUsage.CPU_USED, cpuUsed));
    }
  }
}
