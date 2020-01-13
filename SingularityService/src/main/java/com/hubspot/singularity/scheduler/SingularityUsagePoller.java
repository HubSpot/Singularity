package com.hubspot.singularity.scheduler;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.inject.Inject;
import com.hubspot.singularity.*;
import com.hubspot.singularity.async.CompletableFutures;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.DisasterManager;
import com.hubspot.singularity.data.usage.UsageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class SingularityUsagePoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityUsagePoller.class);

  private final SingularityConfiguration configuration;
  private final UsageManager usageManager;
  private final SingularityUsageHelper usageHelper;
  private final DeployManager deployManager;
  private final DisasterManager disasterManager;
  private final ExecutorService usageExecutor;
  private final SingularityTaskShuffler taskShuffler;

  @Inject
  SingularityUsagePoller(SingularityConfiguration configuration,
                         SingularityUsageHelper usageHelper,
                         UsageManager usageManager,
                         DeployManager deployManager,
                         DisasterManager disasterManager,
                         SingularityManagedThreadPoolFactory threadPoolFactory,
                         SingularityTaskShuffler taskShuffler) {
    super(configuration.getCheckUsageEveryMillis(), TimeUnit.MILLISECONDS);

    this.configuration = configuration;
    this.usageHelper = usageHelper;
    this.usageManager = usageManager;
    this.deployManager = deployManager;
    this.disasterManager = disasterManager;
    this.taskShuffler = taskShuffler;

    this.usageExecutor = threadPoolFactory.get("usage-collection", configuration.getMaxConcurrentUsageCollections());
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
      usageFutures.add(CompletableFuture.runAsync(() -> {
            usageHelper.collectSlaveUsage(slave, now, utilizationPerRequestId, previousUtilizations, overLoadedHosts, totalMemBytesUsed, totalMemBytesAvailable,
                totalCpuUsed, totalCpuAvailable, totalDiskBytesUsed, totalDiskBytesAvailable, false);
          }, usageExecutor)
      );
    });

    CompletableFutures.allOf(usageFutures).join();

    usageManager.saveClusterUtilization(
        getClusterUtilization(
            utilizationPerRequestId, totalMemBytesUsed.get(), totalMemBytesAvailable.get(),
            totalCpuUsed.get(), totalCpuAvailable.get(), totalDiskBytesUsed.get(), totalDiskBytesAvailable.get(), now));
    utilizationPerRequestId.values().forEach(usageManager::saveRequestUtilization);

    if (configuration.isShuffleTasksForOverloadedSlaves() && !disasterManager.isDisabled(SingularityAction.TASK_SHUFFLE)) {
      taskShuffler.shuffle(overLoadedHosts);
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
