package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
import com.hubspot.singularity.SingularityDeleteResult;
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
    long totalDiskBytesUsed = 0;
    long totalDiskBytesAvailable = 0;

    for (SingularitySlave slave : usageHelper.getSlavesToTrackUsageFor()) {
      Map<ResourceUsageType, Number> longRunningTasksUsage = new HashMap<>();
      longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
      longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
      longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, 0);

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

          SingularityTaskUsage latestUsage = getUsage(taskUsage);
          List<SingularityTaskUsage> pastTaskUsages = usageManager.getTaskUsage(taskId);


          clearOldUsage(taskId);
          usageManager.saveSpecificTaskUsage(taskId, latestUsage);

          Optional<SingularityTask> maybeTask = taskManager.getTask(task);
          if (maybeTask.isPresent() && maybeTask.get().getTaskRequest().getDeploy().getResources().isPresent()) {
            double memoryMbReservedForTask = maybeTask.get().getTaskRequest().getDeploy().getResources().get().getMemoryMb();
            double cpuReservedForTask = maybeTask.get().getTaskRequest().getDeploy().getResources().get().getCpus();
            double diskMbReservedForTask = maybeTask.get().getTaskRequest().getDeploy().getResources().get().getDiskMb();

            memoryMbReservedOnSlave += memoryMbReservedForTask;
            cpuReservedOnSlave += cpuReservedForTask;
            diskMbReservedOnSlave += diskMbReservedForTask;

            updateRequestUtilization(utilizationPerRequestId, pastTaskUsages, latestUsage, task, memoryMbReservedForTask, cpuReservedForTask, diskMbReservedForTask);
          }
          memoryBytesUsedOnSlave += latestUsage.getMemoryTotalBytes();
          diskMbUsedOnSlave += latestUsage.getDiskTotalBytes();

          if (!pastTaskUsages.isEmpty()) {
            SingularityTaskUsage lastUsage = pastTaskUsages.get(pastTaskUsages.size() - 1);

            double taskCpusUsed = ((latestUsage.getCpuSeconds() - lastUsage.getCpuSeconds()) / (latestUsage.getTimestamp() - lastUsage.getTimestamp()));

            if (isLongRunning(task) ||  isConsideredLongRunning(task)) {
              updateLongRunningTasksUsage(longRunningTasksUsage, latestUsage.getMemoryTotalBytes(), taskCpusUsed, latestUsage.getDiskTotalBytes());
            }
            SingularityTaskCurrentUsage currentUsage = new SingularityTaskCurrentUsage(latestUsage.getMemoryTotalBytes(), now, taskCpusUsed);

            usageManager.saveCurrentTaskUsage(taskId, currentUsage);

            cpusUsedOnSlave += taskCpusUsed;
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

        SingularitySlaveUsage slaveUsage = new SingularitySlaveUsage(cpusUsedOnSlave, cpuReservedOnSlave, cpusTotal, memoryBytesUsedOnSlave, memoryMbReservedOnSlave, memoryMbTotal, diskMbUsedOnSlave, diskMbReservedOnSlave, diskMbTotal, longRunningTasksUsage, allTaskUsage.size(), now);
        List<Long> slaveTimestamps = usageManager.getSlaveUsageTimestamps(slave.getId());
        if (slaveTimestamps.size() + 1 > configuration.getNumUsageToKeep()) {
          usageManager.deleteSpecificSlaveUsage(slave.getId(), slaveTimestamps.get(0));
        }

        if (slaveUsage.getMemoryBytesTotal().isPresent() && slaveUsage.getCpusTotal().isPresent()) {
          totalMemBytesUsed += slaveUsage.getMemoryBytesUsed();
          totalCpuUsed += slaveUsage.getCpusUsed();
          totalDiskBytesUsed += slaveUsage.getDiskBytesUsed();

          totalMemBytesAvailable += slaveUsage.getMemoryBytesTotal().get();
          totalCpuAvailable += slaveUsage.getCpusTotal().get();
          totalDiskBytesAvailable += slaveUsage.getDiskBytesTotal().get();
        }

        LOG.debug("Saving slave {} usage {}", slave.getHost(), slaveUsage);
        usageManager.saveSpecificSlaveUsageAndSetCurrent(slave.getId(), slaveUsage);
      } catch (Exception e) {
        String message = String.format("Could not get slave usage for host %s", slave.getHost());
        LOG.error(message, e);
        exceptionNotifier.notify(message, e);
      }
    }
    usageManager.saveClusterUtilization(getClusterUtilization(utilizationPerRequestId, totalMemBytesUsed, totalMemBytesAvailable, totalCpuUsed, totalCpuAvailable, totalDiskBytesUsed, totalDiskBytesAvailable, now));
  }

  private SingularityTaskUsage getUsage(MesosTaskMonitorObject taskUsage) {
    double cpuSeconds = taskUsage.getStatistics().getCpusSystemTimeSecs() + taskUsage.getStatistics().getCpusUserTimeSecs();

    return new SingularityTaskUsage(taskUsage.getStatistics().getMemTotalBytes(), taskUsage.getStatistics().getTimestampSeconds(), cpuSeconds, taskUsage.getStatistics().getDiskUsedBytes());
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

  private void updateLongRunningTasksUsage(Map<ResourceUsageType, Number> longRunningTasksUsage, long memBytesUsed, double cpuUsed, long diskBytesUsed) {
    longRunningTasksUsage.compute(ResourceUsageType.MEMORY_BYTES_USED, (k, v) -> (v == null) ? memBytesUsed : v.longValue() + memBytesUsed);
    longRunningTasksUsage.compute(ResourceUsageType.CPU_USED, (k, v) -> (v == null) ? cpuUsed : v.doubleValue() + cpuUsed);
    longRunningTasksUsage.compute(ResourceUsageType.DISK_BYTES_USED, (k, v) -> (v == null) ? diskBytesUsed : v.doubleValue() + diskBytesUsed);
  }

  private void updateRequestUtilization(Map<String, RequestUtilization> utilizationPerRequestId,
                                        List<SingularityTaskUsage> pastTaskUsages,
                                        SingularityTaskUsage latestUsage,
                                        SingularityTaskId task,
                                        double memoryMbReservedForTask,
                                        double cpuReservedForTask,
                                        double diskMbReservedForTask) {
    String requestId = task.getRequestId();
    RequestUtilization requestUtilization = utilizationPerRequestId.getOrDefault(requestId, new RequestUtilization(requestId, task.getDeployId()));
    long curMaxMemBytesUsed = 0;
    long curMinMemBytesUsed = Long.MAX_VALUE;
    double curMaxCpuUsed = 0;
    double curMinCpuUsed = Double.MAX_VALUE;
    long curMaxDiskBytesUsed = 0;
    long curMinDiskBytesUsed = Long.MAX_VALUE;

    if (utilizationPerRequestId.containsKey(requestId)) {
      curMaxMemBytesUsed = requestUtilization.getMaxMemBytesUsed();
      curMinMemBytesUsed = requestUtilization.getMinMemBytesUsed();
      curMaxCpuUsed = requestUtilization.getMaxCpuUsed();
      curMinCpuUsed = requestUtilization.getMinCpuUsed();
      curMaxDiskBytesUsed = requestUtilization.getMaxDiskBytesUsed();
      curMinDiskBytesUsed = requestUtilization.getMinDiskBytesUsed();
    }

    List<SingularityTaskUsage> pastTaskUsagesCopy = copyUsages(pastTaskUsages, latestUsage, task);
    int numTasks = pastTaskUsagesCopy.size() - 1;

    for (int i = 0; i < numTasks; i++) {
      SingularityTaskUsage olderUsage = pastTaskUsagesCopy.get(i);
      SingularityTaskUsage newerUsage = pastTaskUsagesCopy.get(i + 1);
      double cpusUsed = (newerUsage.getCpuSeconds() - olderUsage.getCpuSeconds()) / (newerUsage.getTimestamp() - olderUsage.getTimestamp());

      curMaxCpuUsed = Math.max(cpusUsed, curMaxCpuUsed);
      curMinCpuUsed = Math.min(cpusUsed, curMinCpuUsed);
      curMaxMemBytesUsed = Math.max(newerUsage.getMemoryTotalBytes(), curMaxMemBytesUsed);
      curMinMemBytesUsed = Math.min(newerUsage.getMemoryTotalBytes(), curMinMemBytesUsed);
      curMaxDiskBytesUsed = Math.max(newerUsage.getDiskTotalBytes(), curMaxDiskBytesUsed);
      curMinDiskBytesUsed = Math.min(newerUsage.getDiskTotalBytes(), curMinDiskBytesUsed);

      requestUtilization
          .addCpuUsed(cpusUsed)
          .addMemBytesUsed(newerUsage.getMemoryTotalBytes())
          .addDiskBytesUsed(newerUsage.getDiskTotalBytes())
          .incrementTaskCount();
    }

    requestUtilization
        .addMemBytesReserved((long) (memoryMbReservedForTask * SingularitySlaveUsage.BYTES_PER_MEGABYTE * numTasks))
        .addCpuReserved(cpuReservedForTask * numTasks)
        .addDiskBytesReserved((long) diskMbReservedForTask * SingularitySlaveUsage.BYTES_PER_MEGABYTE  * numTasks)
        .setMaxCpuUsed(curMaxCpuUsed)
        .setMinCpuUsed(curMinCpuUsed)
        .setMaxMemBytesUsed(curMaxMemBytesUsed)
        .setMinMemBytesUsed(curMinMemBytesUsed)
        .setMaxDiskBytesUsed(curMaxDiskBytesUsed)
        .setMinDiskBytesUsed(curMinDiskBytesUsed);

    utilizationPerRequestId.put(requestId, requestUtilization);
  }

  private List<SingularityTaskUsage> copyUsages(List<SingularityTaskUsage> pastTaskUsages, SingularityTaskUsage latestUsage, SingularityTaskId task) {
    List<SingularityTaskUsage> pastTaskUsagesCopy = new ArrayList<>();
    pastTaskUsagesCopy.add(new SingularityTaskUsage(0, TimeUnit.MILLISECONDS.toSeconds(task.getStartedAt()), 0, 0)); // to calculate oldest cpu usage
    pastTaskUsagesCopy.addAll(pastTaskUsages);
    pastTaskUsagesCopy.add(latestUsage);

    return pastTaskUsagesCopy;
  }

  private SingularityClusterUtilization getClusterUtilization(Map<String, RequestUtilization> utilizationPerRequestId,
                                                              long totalMemBytesUsed,
                                                              long totalMemBytesAvailable,
                                                              double totalCpuUsed,
                                                              double totalCpuAvailable,
                                                              long totalDiskBytesUsed,
                                                              long totalDiskBytesAvailable,
                                                              long now) {
    int numRequestsWithUnderUtilizedCpu = 0;
    int numRequestsWithOverUtilizedCpu = 0;
    int numRequestsWithUnderUtilizedMemBytes = 0;
    int numRequestsWithUnderUtilizedDiskBytes = 0;

    double totalUnderUtilizedCpu = 0;
    double totalOverUtilizedCpu = 0;
    long totalUnderUtilizedMemBytes = 0;
    long totalUnderUtilizedDiskBytes = 0;

    double maxUnderUtilizedCpu = 0;
    double maxOverUtilizedCpu = 0;
    long maxUnderUtilizedMemBytes = 0;
    long maxUnderUtilizedDiskBytes = 0;

    String maxUnderUtilizedCpuRequestId = null;
    String maxOverUtilizedCpuRequestId = null;
    String maxUnderUtilizedMemBytesRequestId = null;
    String maxUnderUtilizedDiskBytesRequestId = null;

    double minUnderUtilizedCpu = Double.MAX_VALUE;
    double minOverUtilizedCpu = Double.MAX_VALUE;
    long minUnderUtilizedMemBytes = Long.MAX_VALUE;
    long minUnderUtilizedDiskBytes = Long.MAX_VALUE;


    for (RequestUtilization utilization : utilizationPerRequestId.values()) {
      Optional<SingularityDeploy> maybeDeploy = deployManager.getDeploy(utilization.getRequestId(), utilization.getDeployId());

      if (maybeDeploy.isPresent() && maybeDeploy.get().getResources().isPresent()) {
        String requestId = utilization.getRequestId();
        long memoryBytesReserved = (long) (maybeDeploy.get().getResources().get().getMemoryMb() * SingularitySlaveUsage.BYTES_PER_MEGABYTE);
        double cpuReserved = maybeDeploy.get().getResources().get().getCpus();
        long diskBytesReserved = (long) maybeDeploy.get().getResources().get().getDiskMb() * SingularitySlaveUsage.BYTES_PER_MEGABYTE;

        double unusedCpu = cpuReserved - utilization.getAvgCpuUsed();
        long unusedMemBytes = (long) (memoryBytesReserved - utilization.getAvgMemBytesUsed());
        long unusedDiskBytes = (long) (diskBytesReserved - utilization.getAvgDiskBytesUsed());

        if (unusedCpu > 0) {
          numRequestsWithUnderUtilizedCpu++;
          totalUnderUtilizedCpu += unusedCpu;
          if (unusedCpu > maxUnderUtilizedCpu) {
            maxUnderUtilizedCpu = unusedCpu;
            maxUnderUtilizedCpuRequestId = requestId;
          }
          minUnderUtilizedCpu = Math.min(unusedCpu, minUnderUtilizedCpu);
        } else if (unusedCpu < 0) {
          double overusedCpu = Math.abs(unusedCpu);

          numRequestsWithOverUtilizedCpu++;
          totalOverUtilizedCpu += overusedCpu;
          if (overusedCpu > maxOverUtilizedCpu) {
            maxOverUtilizedCpu = overusedCpu;
            maxOverUtilizedCpuRequestId = requestId;
          }
          minOverUtilizedCpu = Math.min(overusedCpu, minOverUtilizedCpu);
        }

        if (unusedMemBytes > 0) {
          numRequestsWithUnderUtilizedMemBytes++;
          totalUnderUtilizedMemBytes += unusedMemBytes;
          if (unusedMemBytes > maxUnderUtilizedMemBytes) {
            maxUnderUtilizedMemBytes = unusedMemBytes;
            maxUnderUtilizedMemBytesRequestId = requestId;
          }
          minUnderUtilizedMemBytes = Math.min(unusedMemBytes, minUnderUtilizedMemBytes);
        }

        if (unusedDiskBytes > 0) {
          numRequestsWithUnderUtilizedDiskBytes++;
          totalUnderUtilizedDiskBytes += unusedDiskBytes;
          if (unusedDiskBytes > maxUnderUtilizedDiskBytes) {
            maxUnderUtilizedDiskBytes = unusedDiskBytes;
            maxUnderUtilizedDiskBytesRequestId = requestId;
          }
          minUnderUtilizedDiskBytes = Math.min(unusedDiskBytes, minUnderUtilizedMemBytes);
        }
      }
    }

    double avgUnderUtilizedCpu = numRequestsWithUnderUtilizedCpu != 0 ? totalUnderUtilizedCpu / numRequestsWithUnderUtilizedCpu : 0;
    double avgOverUtilizedCpu = numRequestsWithOverUtilizedCpu != 0? totalOverUtilizedCpu / numRequestsWithOverUtilizedCpu : 0;
    long avgUnderUtilizedMemBytes = numRequestsWithUnderUtilizedMemBytes != 0 ? totalUnderUtilizedMemBytes / numRequestsWithUnderUtilizedMemBytes : 0;
    long avgUnderUtilizedDiskBytes = numRequestsWithUnderUtilizedDiskBytes != 0 ? totalUnderUtilizedDiskBytes / numRequestsWithUnderUtilizedDiskBytes : 0;

    return new SingularityClusterUtilization(new ArrayList<>(utilizationPerRequestId.values()), numRequestsWithUnderUtilizedCpu, numRequestsWithOverUtilizedCpu,
        numRequestsWithUnderUtilizedMemBytes, numRequestsWithUnderUtilizedDiskBytes, totalUnderUtilizedCpu, totalOverUtilizedCpu, totalUnderUtilizedMemBytes, totalUnderUtilizedDiskBytes, avgUnderUtilizedCpu, avgOverUtilizedCpu,
        avgUnderUtilizedMemBytes, avgUnderUtilizedDiskBytes, maxUnderUtilizedCpu, maxOverUtilizedCpu, maxUnderUtilizedMemBytes, maxUnderUtilizedDiskBytes, maxUnderUtilizedCpuRequestId, maxOverUtilizedCpuRequestId,
        maxUnderUtilizedMemBytesRequestId, maxUnderUtilizedDiskBytesRequestId, getMin(minUnderUtilizedCpu), getMin(minOverUtilizedCpu), getMin(minUnderUtilizedMemBytes), getMin(minUnderUtilizedDiskBytes), totalMemBytesUsed,
        totalMemBytesAvailable, totalDiskBytesUsed, totalDiskBytesAvailable, totalCpuUsed, totalCpuAvailable, now);
  }

  private double getMin(double value) {
    return value == Double.MAX_VALUE ? 0 : value;
  }

  private long getMin(long value) {
    return value == Long.MAX_VALUE ? 0 : value;
  }

  @VisibleForTesting
  void clearOldUsage(String taskId) {
    List<Double> pastTaskUsagePaths = usageManager.getTaskUsagePaths(taskId).stream().map(Double::parseDouble).collect(Collectors.toList());

    while (pastTaskUsagePaths.size() + 1 > configuration.getNumUsageToKeep()) {
      long minSecondsApart = configuration.getUsageIntervalSeconds();
      boolean deleted = false;

      for (int i = 0; i < pastTaskUsagePaths.size() - 1; i++) {
        if (pastTaskUsagePaths.get(i + 1) - pastTaskUsagePaths.get(i) < minSecondsApart) {
          SingularityDeleteResult result = usageManager.deleteSpecificTaskUsage(taskId, pastTaskUsagePaths.get(i + 1));

          if (result.equals(SingularityDeleteResult.DIDNT_EXIST)) {
            LOG.warn("Didn't delete taskUsage {} for taskId {}", pastTaskUsagePaths.get(i + 1).toString(), taskId);
          }

          deleted = true;
          pastTaskUsagePaths.remove(pastTaskUsagePaths.get(i + 1));
          break;
        }
      }

      if (!deleted) {
        usageManager.deleteSpecificTaskUsage(taskId, pastTaskUsagePaths.get(0));
        pastTaskUsagePaths.remove(pastTaskUsagePaths.get(0));
      }
    }

  }
}
