package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
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

import com.google.common.base.Optional;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.singularity.RequestUtilization;
import com.hubspot.singularity.SingularityClusterUtilization;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.async.AsyncSemaphore;
import com.hubspot.singularity.async.CompletableFutures;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.UsageManager;

public class SingularityUsagePoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityUsagePoller.class);

  private final SingularityConfiguration configuration;
  private final UsageManager usageManager;
  private final SingularityUsageHelper usageHelper;
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
                         RequestManager requestManager,
                         DeployManager deployManager,
                         TaskManager taskManager) {
    super(configuration.getCheckUsageEveryMillis(), TimeUnit.MILLISECONDS);

    this.configuration = configuration;
    this.usageHelper = usageHelper;
    this.usageManager = usageManager;
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
    Map<String, RequestUtilization> previousUtilizations = usageManager.getRequestUtilizations(false);
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
            usageHelper.collectSlaveUsage(slave, now, utilizationPerRequestId, previousUtilizations, overLoadedHosts, totalMemBytesUsed, totalMemBytesAvailable,
                totalCpuUsed, totalCpuAvailable, totalDiskBytesUsed, totalDiskBytesAvailable, false);
          }, usageExecutor)
      ));
    });

    CompletableFutures.allOf(usageFutures).join();

    usageManager.saveClusterUtilization(
        getClusterUtilization(
            utilizationPerRequestId, totalMemBytesUsed.get(), totalMemBytesAvailable.get(),
            totalCpuUsed.get(), totalCpuAvailable.get(), totalDiskBytesUsed.get(), totalDiskBytesAvailable.get(), now));
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
        Optional<String> message = Optional.of(String.format(
            "Load on slave is %s / %s, shuffling task using %s / %s to less busy host",
            systemLoad,
            overloadedSlave.getSystemCpusTotal(),
            taskIdWithUsage.getUsage().getCpusUsed(),
            taskIdWithUsage.getRequestedResources().getCpus()));
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

}
