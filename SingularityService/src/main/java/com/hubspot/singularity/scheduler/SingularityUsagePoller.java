package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.client.MesosClient;
import com.hubspot.mesos.json.MesosSlaveMetricsSnapshotObject;
import com.hubspot.mesos.json.MesosTaskMonitorObject;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.InvalidSingularityTaskIdException;
import com.hubspot.singularity.RequestUtilization;
import com.hubspot.singularity.SingularityClusterUtilization;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularitySlaveUsage.ResourceUsageType;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskCurrentUsage;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.async.AsyncSemaphore;
import com.hubspot.singularity.async.CompletableFutures;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.UsageManager;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

public class SingularityUsagePoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityUsagePoller.class);
  private static final long DAY_IN_SECONDS = TimeUnit.DAYS.toSeconds(1);

  private final SingularityConfiguration configuration;
  private final MesosClient mesosClient;
  private final UsageManager usageManager;
  private final SingularityUsageHelper usageHelper;
  private final SingularityExceptionNotifier exceptionNotifier;
  private final RequestManager requestManager;
  private final DeployManager deployManager;
  private final TaskManager taskManager;

  private final AsyncSemaphore<Void> usageCollectionSemaphore;
  private final ExecutorService usageExecutor;
  private final ConcurrentHashMap<String, ReentrantLock> requestLocks;

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

    this.usageCollectionSemaphore = AsyncSemaphore.newBuilder(configuration::getMaxConcurrentUsageCollections).build();
    this.usageExecutor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("usage-collection-%d").build());
    this.requestLocks = new ConcurrentHashMap<>();
  }

  @Override
  public void runActionOnPoll() {
    Map<String, RequestUtilization> utilizationPerRequestId = new ConcurrentHashMap<>();
    Map<String, RequestUtilization> previousUtilizations = usageManager.getRequestUtilizations();
    final long now = System.currentTimeMillis();

    AtomicLong totalMemBytesUsed = new AtomicLong(0);
    AtomicLong totalMemBytesAvailable = new AtomicLong(0);
    AtomicDouble totalCpuUsed = new AtomicDouble(0.00);
    AtomicDouble totalCpuAvailable = new AtomicDouble(0.00);
    AtomicLong totalDiskBytesUsed = new AtomicLong(0);
    AtomicLong totalDiskBytesAvailable = new AtomicLong(0);

    Map<SingularitySlaveUsage, List<TaskIdWithUsage>> overLoadedHosts = new ConcurrentHashMap<>();

    List<CompletableFuture<Void>> usageFutures = new ArrayList<>();

    usageHelper.getSlavesToTrackUsageFor().forEach((slave) -> {
      usageFutures.add(usageCollectionSemaphore.call(() ->
          CompletableFuture.runAsync(() -> {
            collectSlaveUage(slave, now, utilizationPerRequestId, previousUtilizations, overLoadedHosts, totalMemBytesUsed, totalMemBytesAvailable,
                totalCpuUsed, totalCpuAvailable, totalDiskBytesUsed, totalDiskBytesAvailable);
          }, usageExecutor)
      ));
    });

    CompletableFutures.allOf(usageFutures).join();

    usageManager.saveClusterUtilization(
        getClusterUtilization(utilizationPerRequestId, totalMemBytesUsed.get(), totalMemBytesAvailable.get(), totalCpuUsed.get(), totalCpuAvailable.get(), totalDiskBytesUsed.get(), totalDiskBytesAvailable
            .get(), now));
    utilizationPerRequestId.values().forEach(usageManager::saveRequestUtilization);

    if (configuration.isShuffleTasksForOverloadedSlaves()) {
      shuffleTasksOnOverloadedHosts(overLoadedHosts);
    }
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

  private void collectSlaveUage(SingularitySlave slave,
                                long now,
                                Map<String, RequestUtilization> utilizationPerRequestId,
                                Map<String, RequestUtilization> previousUtilizations,
                                Map<SingularitySlaveUsage, List<TaskIdWithUsage>> overLoadedHosts,
                                AtomicLong totalMemBytesUsed,
                                AtomicLong totalMemBytesAvailable,
                                AtomicDouble totalCpuUsed,
                                AtomicDouble totalCpuAvailable,
                                AtomicLong totalDiskBytesUsed,
                                AtomicLong totalDiskBytesAvailable) {
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

            runWithRequestLock(() -> updateRequestUtilization(utilizationPerRequestId, previousUtilizations, pastTaskUsages, latestUsage, task, memoryMbReservedForTask, cpuReservedForTask, diskMbReservedForTask), task.getRequestId());
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
            if (isLongRunning(task) || isConsideredLongRunning(task)) {
              updateLongRunningTasksUsage(longRunningTasksUsage, latestUsage.getMemoryTotalBytes(), usedCpusSinceStart, latestUsage.getDiskTotalBytes());
            }
            currentUsage = new SingularityTaskCurrentUsage(latestUsage.getMemoryTotalBytes(), now, usedCpusSinceStart, latestUsage.getDiskTotalBytes());
            usageManager.saveCurrentTaskUsage(taskId, currentUsage);

            cpusUsedOnSlave += usedCpusSinceStart;
          }
        } else {
          SingularityTaskUsage lastUsage = pastTaskUsages.get(pastTaskUsages.size() - 1);

          double taskCpusUsed = ((latestUsage.getCpuSeconds() - lastUsage.getCpuSeconds()) / (latestUsage.getTimestamp() - lastUsage.getTimestamp()));

          if (isLongRunning(task) || isConsideredLongRunning(task)) {
            updateLongRunningTasksUsage(longRunningTasksUsage, latestUsage.getMemoryTotalBytes(), taskCpusUsed, latestUsage.getDiskTotalBytes());
          }

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
          memoryMbTotal, diskMbUsedOnSlave, diskMbReservedOnSlave, diskMbTotal, longRunningTasksUsage, allTaskUsage.size(), now,
          systemMemTotalBytes, systemMemFreeBytes, systemCpusTotal, systemLoad1Min, systemLoad5Min, systemLoad15Min, slaveDiskUsed, slaveDiskTotal);

      if (slaveOverloaded) {
        overLoadedHosts.put(slaveUsage, possibleTasksToShuffle);
      }

      List<Long> slaveTimestamps = usageManager.getSlaveUsageTimestamps(slave.getId());
      if (slaveTimestamps.size() + 1 > configuration.getNumUsageToKeep()) {
        usageManager.deleteSpecificSlaveUsage(slave.getId(), slaveTimestamps.get(0));
      }

      if (slaveUsage.getMemoryBytesTotal().isPresent() && slaveUsage.getCpusTotal().isPresent()) {
        totalMemBytesUsed.getAndAdd(slaveUsage.getMemoryBytesUsed());
        totalCpuUsed.getAndAdd(slaveUsage.getCpusUsed());
        totalDiskBytesUsed.getAndAdd(slaveUsage.getDiskBytesUsed());

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

  private void shuffleTasksOnOverloadedHosts(Map<SingularitySlaveUsage, List<TaskIdWithUsage>> overLoadedHosts) {
    List<SingularityTaskCleanup> shuffleCleanups = taskManager.getCleanupTasks()
        .stream()
        .filter((taskCleanup) -> taskCleanup.getCleanupType() == TaskCleanupType.REBALANCE_CPU_USAGE)
        .collect(Collectors.toList());
    long currentShuffleCleanupsTotal = shuffleCleanups.size();
    Set<String> requestsWithShuffledTasks = shuffleCleanups
        .stream()
        .map((taskCleanup) -> taskCleanup.getTaskId().getRequestId())
        .collect(Collectors.toSet());

    List<SingularitySlaveUsage> overLoadedSlavesByUsage = overLoadedHosts.keySet().stream()
        .sorted((usage1, usage2) -> Double.compare(
            getSystemLoadForShuffle(usage2),
            getSystemLoadForShuffle(usage1)
        ))
        .collect(Collectors.toList());
    for (SingularitySlaveUsage overloadedSlave : overLoadedSlavesByUsage) {
      if (currentShuffleCleanupsTotal >= configuration.getMaxTasksToShuffleTotal()) {
        LOG.debug("Not shuffling any more tasks (totalShuffleCleanups: {})", currentShuffleCleanupsTotal);
        break;
      }
      int shuffledTasksOnSlave = 0;
      List<TaskIdWithUsage> possibleTasksToShuffle = overLoadedHosts.get(overloadedSlave);
      possibleTasksToShuffle.sort((u1, u2) ->
          Double.compare(
              u2.getUsage().getCpusUsed() / u2.getRequestedResources().getCpus(),
              u1.getUsage().getCpusUsed() / u1.getRequestedResources().getCpus()
          ));

      double systemLoad = getSystemLoadForShuffle(overloadedSlave);
      double cpuOverage = systemLoad - overloadedSlave.getSystemCpusTotal();

      for (TaskIdWithUsage taskIdWithUsage : possibleTasksToShuffle) {
        if (requestsWithShuffledTasks.contains(taskIdWithUsage.getTaskId().getRequestId())) {
          LOG.debug("Request {} already has a shuffling task, skipping", taskIdWithUsage.getTaskId().getRequestId());
          continue;
        }
        if (cpuOverage <= 0 || shuffledTasksOnSlave > configuration.getMaxTasksToShufflePerHost() || currentShuffleCleanupsTotal >= configuration.getMaxTasksToShuffleTotal()) {
          LOG.debug("Not shuffling any more tasks (overage: {}, shuffledOnHost: {}, totalShuffleCleanups: {})", cpuOverage, shuffledTasksOnSlave, currentShuffleCleanupsTotal);
          break;
        }
        LOG.debug("Cleaning up task {} to free up cpu on overloaded host (remaining cpu overage: {})", taskIdWithUsage.getTaskId(), cpuOverage);
        Optional<String> message = Optional.of(String.format("Load on slave is %s / %s, shuffling task to less busy host", systemLoad, overloadedSlave.getSystemCpusTotal()));
        taskManager.createTaskCleanup(
            new SingularityTaskCleanup(
                Optional.absent(),
                TaskCleanupType.REBALANCE_CPU_USAGE,
                System.currentTimeMillis(),
                taskIdWithUsage.getTaskId(),
                message,
                Optional.of(UUID.randomUUID().toString()),
                Optional.absent(), Optional.absent()));
        requestManager.addToPendingQueue(new SingularityPendingRequest(taskIdWithUsage.getTaskId().getRequestId(), taskIdWithUsage.getTaskId()
            .getDeployId(), System.currentTimeMillis(), Optional.absent(),
            PendingType.TASK_BOUNCE, Optional.absent(), Optional.absent(), Optional.absent(), message, Optional.of(UUID.randomUUID().toString())));
        cpuOverage -= taskIdWithUsage.getUsage().getCpusUsed();
        shuffledTasksOnSlave++;
        currentShuffleCleanupsTotal++;
        requestsWithShuffledTasks.add(taskIdWithUsage.getTaskId().getRequestId());
      }
    }
  }

  private double getSystemLoadForShuffle(SingularitySlaveUsage usage) {
    switch (configuration.getMesosConfiguration().getScoreUsingSystemLoad()) {
      case LOAD_1:
        return usage.getSystemLoad15Min();
      case LOAD_15:
        return usage.getSystemLoad15Min();
      case LOAD_5:
      default:
        return usage.getSystemLoad5Min();
    }
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
                                        Map<String, RequestUtilization> previousUtilizations,
                                        List<SingularityTaskUsage> pastTaskUsages,
                                        SingularityTaskUsage latestUsage,
                                        SingularityTaskId task,
                                        double memoryMbReservedForTask,
                                        double cpuReservedForTask,
                                        double diskMbReservedForTask) {
    String requestId = task.getRequestId();
    RequestUtilization newRequestUtilization = utilizationPerRequestId.getOrDefault(requestId, new RequestUtilization(requestId, task.getDeployId()));
    RequestUtilization previous = previousUtilizations.get(requestId);
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

  private List<SingularityTaskUsage> getFullListOfTaskUsages(List<SingularityTaskUsage> pastTaskUsages, SingularityTaskUsage latestUsage, SingularityTaskId task) {
    List<SingularityTaskUsage> pastTaskUsagesCopy = new ArrayList<>();
    pastTaskUsagesCopy.add(new SingularityTaskUsage(0, TimeUnit.MILLISECONDS.toSeconds(task.getStartedAt()), 0, 0, 0 , 0, 0)); // to calculate oldest cpu usage
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
    double avgOverUtilizedCpu = numRequestsWithOverUtilizedCpu != 0 ? totalOverUtilizedCpu / numRequestsWithOverUtilizedCpu : 0;
    long avgUnderUtilizedMemBytes = numRequestsWithUnderUtilizedMemBytes != 0 ? totalUnderUtilizedMemBytes / numRequestsWithUnderUtilizedMemBytes : 0;
    long avgUnderUtilizedDiskBytes = numRequestsWithUnderUtilizedDiskBytes != 0 ? totalUnderUtilizedDiskBytes / numRequestsWithUnderUtilizedDiskBytes : 0;

    return new SingularityClusterUtilization(numRequestsWithUnderUtilizedCpu, numRequestsWithOverUtilizedCpu,
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

  private static class TaskIdWithUsage {
    private final SingularityTaskId taskId;
    private final Resources requestedResources;
    private final SingularityTaskCurrentUsage usage;

    TaskIdWithUsage(SingularityTaskId taskId, Resources requestedResources, SingularityTaskCurrentUsage usage) {
      this.taskId = taskId;
      this.requestedResources = requestedResources;
      this.usage = usage;
    }

    public SingularityTaskId getTaskId() {
      return taskId;
    }

    public Resources getRequestedResources() {
      return requestedResources;
    }

    public SingularityTaskCurrentUsage getUsage() {
      return usage;
    }
  }
}
