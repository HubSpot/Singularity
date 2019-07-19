package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.inject.Inject;
import com.hubspot.singularity.RequestUtilization;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityClusterUtilization;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityManagedCachedThreadPoolFactory;
import com.hubspot.singularity.SingularityManagedScheduledExecutorServiceFactory;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.async.AsyncSemaphore;
import com.hubspot.singularity.async.CompletableFutures;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.DisasterManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.usage.UsageManager;
import com.hubspot.singularity.scheduler.SingularityUsagePoller.OverusedResource.Type;

import io.dropwizard.util.SizeUnit;

public class SingularityUsagePoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityUsagePoller.class);

  private final SingularityConfiguration configuration;
  private final UsageManager usageManager;
  private final SingularityUsageHelper usageHelper;
  private final RequestManager requestManager;
  private final DeployManager deployManager;
  private final TaskManager taskManager;
  private final DisasterManager disasterManager;

  private final AsyncSemaphore<Void> usageCollectionSemaphore;
  private final ExecutorService usageExecutor;

  @Inject
  SingularityUsagePoller(SingularityConfiguration configuration,
                         SingularityUsageHelper usageHelper,
                         UsageManager usageManager,
                         RequestManager requestManager,
                         DeployManager deployManager,
                         TaskManager taskManager,
                         DisasterManager disasterManager,
                         SingularityManagedScheduledExecutorServiceFactory executorServiceFactory,
                         SingularityManagedCachedThreadPoolFactory cachedThreadPoolFactory) {
    super(configuration.getCheckUsageEveryMillis(), TimeUnit.MILLISECONDS);

    this.configuration = configuration;
    this.usageHelper = usageHelper;
    this.usageManager = usageManager;
    this.requestManager = requestManager;
    this.deployManager = deployManager;
    this.taskManager = taskManager;
    this.disasterManager = disasterManager;

    this.usageCollectionSemaphore = AsyncSemaphore.newBuilder(configuration::getMaxConcurrentUsageCollections, executorServiceFactory.get("usage-semaphore", 5)).build();
    this.usageExecutor = cachedThreadPoolFactory.get("usage-collection");
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

    if (configuration.isShuffleTasksForOverloadedSlaves() && !disasterManager.isDisabled(SingularityAction.TASK_SHUFFLE)) {
      shuffleTasksOnOverloadedHosts(overLoadedHosts);
    }
  }

  static class OverusedResource {
    enum Type { MEMORY, CPU };

    double overusage;
    Type resourceType;

    OverusedResource(double overusage, Type resourceType) {
      this.overusage = overusage;
      this.resourceType = resourceType;
    }
  }

  private double getTargetMemoryUtilizationForHost(SingularitySlaveUsage usage) {
    return configuration.getShuffleTasksWhenSlaveMemoryUtilizationPercentageExceeds() * usage.getSystemMemTotalBytes();
  }

  private OverusedResource getMostOverusedResource(SingularitySlaveUsage overloadedSlave, double currentCpuLoad, double currentMemUsageBytes) {
    double cpuOverage = currentCpuLoad - overloadedSlave.getSystemCpusTotal();

    double cpuOverusage = cpuOverage / overloadedSlave.getSystemCpusTotal();

    double targetMemUsageBytes = getTargetMemoryUtilizationForHost(overloadedSlave);
    double memOverageBytes = currentMemUsageBytes - targetMemUsageBytes;
    double memOverusage = memOverageBytes / targetMemUsageBytes;

    if (cpuOverusage > memOverusage) {
      return new OverusedResource(cpuOverusage, Type.CPU);
    } else {
      return new OverusedResource(memOverusage, Type.MEMORY);
    }
  }

  private void shuffleTasksOnOverloadedHosts(Map<SingularitySlaveUsage, List<TaskIdWithUsage>> overLoadedHosts) {
    List<SingularityTaskCleanup> shuffleCleanups = taskManager.getCleanupTasks()
        .stream()
        .filter((taskCleanup) -> taskCleanup.getCleanupType() == TaskCleanupType.REBALANCE_CPU_USAGE || taskCleanup.getCleanupType() == TaskCleanupType.REBALANCE_MEMORY_USAGE)
        .collect(Collectors.toList());
    long currentShuffleCleanupsTotal = shuffleCleanups.size();
    Set<String> requestsWithShuffledTasks = shuffleCleanups
        .stream()
        .map((taskCleanup) -> taskCleanup.getTaskId().getRequestId())
        .collect(Collectors.toSet());

    List<SingularitySlaveUsage> overloadedSlavesByOverusage = overLoadedHosts.keySet().stream()
        .sorted((usage1, usage2) -> {
          OverusedResource mostOverusedResource1 = getMostOverusedResource(usage1, getSystemLoadForShuffle(usage1), usage1.getMemoryBytesUsed());
          OverusedResource mostOverusedResource2 = getMostOverusedResource(usage2, getSystemLoadForShuffle(usage2), usage2.getMemoryBytesUsed());

          return Double.compare(mostOverusedResource2.overusage, mostOverusedResource1.overusage);
        })
        .collect(Collectors.toList());

    for (SingularitySlaveUsage overloadedSlave : overloadedSlavesByOverusage) {
      if (currentShuffleCleanupsTotal >= configuration.getMaxTasksToShuffleTotal()) {
        LOG.debug("Not shuffling any more tasks (totalShuffleCleanups: {})", currentShuffleCleanupsTotal);
        break;
      }
      int shuffledTasksOnSlave = 0;

      double currentCpuLoad = getSystemLoadForShuffle(overloadedSlave);
      double currentMemUsageBytes = overloadedSlave.getSystemMemTotalBytes() - overloadedSlave.getSystemMemFreeBytes();

      OverusedResource mostOverusedResource = getMostOverusedResource(overloadedSlave, currentCpuLoad, currentMemUsageBytes);

      List<TaskIdWithUsage> possibleTasksToShuffle;
      boolean shufflingForCpu;
      if (mostOverusedResource.resourceType == Type.CPU) {
        shufflingForCpu = true;
        possibleTasksToShuffle = overLoadedHosts.get(overloadedSlave);
        possibleTasksToShuffle.sort((u1, u2) ->
            Double.compare(
                u2.getUsage().getCpusUsed() / u2.getRequestedResources().getCpus(),
                u1.getUsage().getCpusUsed() / u1.getRequestedResources().getCpus()
            ));
      } else {
        shufflingForCpu = false;
        possibleTasksToShuffle = overLoadedHosts.get(overloadedSlave);
        possibleTasksToShuffle.sort((u1, u2) ->
            Double.compare(
                u2.getUsage().getMemoryTotalBytes() / u2.getRequestedResources().getMemoryMb(),
                u1.getUsage().getMemoryTotalBytes() / u1.getRequestedResources().getMemoryMb()
            ));
      }

      for (TaskIdWithUsage taskIdWithUsage : possibleTasksToShuffle) {
        if (requestsWithShuffledTasks.contains(taskIdWithUsage.getTaskId().getRequestId())) {
          LOG.debug("Request {} already has a shuffling task, skipping", taskIdWithUsage.getTaskId().getRequestId());
          continue;
        }

        boolean resourceNoLongerOverutilized = (shufflingForCpu && currentCpuLoad <= overloadedSlave.getSystemCpusTotal()) || (!shufflingForCpu && currentMemUsageBytes <= getTargetMemoryUtilizationForHost(overloadedSlave));
        boolean shufflingTooManyTasks = shuffledTasksOnSlave > configuration.getMaxTasksToShufflePerHost() || currentShuffleCleanupsTotal >= configuration.getMaxTasksToShuffleTotal();

        if (resourceNoLongerOverutilized || shufflingTooManyTasks) {
          LOG.debug("Not shuffling any more tasks on slave {} ({} overage : {}%, shuffledOnHost: {}, totalShuffleCleanups: {})", taskIdWithUsage.getTaskId().getSanitizedHost(), mostOverusedResource.resourceType, mostOverusedResource.overusage * 100, shuffledTasksOnSlave, currentShuffleCleanupsTotal);
          break;
        }

        Optional<String> message;

        if (shufflingForCpu) {
          message = Optional.of(String.format(
              "Load on slave is %s / %s, shuffling task using %s / %s to less busy host",
              currentCpuLoad,
              overloadedSlave.getSystemCpusTotal(),
              taskIdWithUsage.getUsage().getCpusUsed(),
              taskIdWithUsage.getRequestedResources().getCpus()));

          currentCpuLoad -= taskIdWithUsage.getUsage().getCpusUsed();
          LOG.debug("Cleaning up task {} to free up cpu on overloaded host (remaining cpu overage: {})", taskIdWithUsage.getTaskId(), currentCpuLoad - overloadedSlave.getSystemCpusTotal());

          taskManager.createTaskCleanup(
              new SingularityTaskCleanup(
                  Optional.empty(),
                  TaskCleanupType.REBALANCE_CPU_USAGE,
                  System.currentTimeMillis(),
                  taskIdWithUsage.getTaskId(),
                  message,
                  Optional.of(UUID.randomUUID().toString()),
                  Optional.empty(), Optional.empty()));
        } else {
          message = Optional.of(String.format(
              "Mem usage on slave is %sMiB / %sMiB, shuffling task using %sMiB / %sMiB to less busy host",
              SizeUnit.BYTES.toMegabytes(((long) currentMemUsageBytes)),
              SizeUnit.BYTES.toMegabytes(((long) overloadedSlave.getSystemMemTotalBytes())),
              SizeUnit.BYTES.toMegabytes(taskIdWithUsage.getUsage().getMemoryTotalBytes()),
              ((long) taskIdWithUsage.getRequestedResources().getMemoryMb())));

          currentMemUsageBytes -= taskIdWithUsage.getUsage().getMemoryTotalBytes();

          LOG.debug("Cleaning up task {} to free up mem on overloaded host (remaining mem overage: {}MiB)", taskIdWithUsage.getTaskId(), SizeUnit.BYTES.toMegabytes(((long) (currentMemUsageBytes - getTargetMemoryUtilizationForHost(overloadedSlave)))));

          taskManager.createTaskCleanup(
              new SingularityTaskCleanup(
                  Optional.empty(),
                  TaskCleanupType.REBALANCE_MEMORY_USAGE,
                  System.currentTimeMillis(),
                  taskIdWithUsage.getTaskId(),
                  message,
                  Optional.of(UUID.randomUUID().toString()),
                  Optional.empty(), Optional.empty()));
        }

        requestManager.addToPendingQueue(new SingularityPendingRequest(taskIdWithUsage.getTaskId().getRequestId(), taskIdWithUsage.getTaskId()
            .getDeployId(), System.currentTimeMillis(), Optional.empty(),
            PendingType.TASK_BOUNCE, Optional.empty(), Optional.empty(), Optional.empty(), message, Optional.of(UUID.randomUUID().toString())));

        shuffledTasksOnSlave++;
        currentShuffleCleanupsTotal++;
        requestsWithShuffledTasks.add(taskIdWithUsage.getTaskId().getRequestId());
      }
    }
  }

  private double getSystemLoadForShuffle(SingularitySlaveUsage usage) {
    switch (configuration.getMesosConfiguration().getScoreUsingSystemLoad()) {
      case LOAD_1:
        return usage.getSystemLoad1Min();
      case LOAD_5:
        return usage.getSystemLoad5Min();
      case LOAD_15:
      default:
        return usage.getSystemLoad15Min();
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
