package com.hubspot.singularity.scheduler;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.scheduler.SingularityTaskShuffler.OverusedResource.Type;
import io.dropwizard.util.SizeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class SingularityTaskShuffler {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityTaskShuffler.class);

  private final SingularityConfiguration configuration;
  private final RequestManager requestManager;
  private final TaskManager taskManager;

  @Inject
  SingularityTaskShuffler(SingularityConfiguration configuration,
                          RequestManager requestManager,
                          TaskManager taskManager) {
    this.configuration = configuration;
    this.requestManager = requestManager;
    this.taskManager = taskManager;
  }

  static class OverusedResource {
    enum Type {MEMORY, CPU}

    double usage;
    double total;
    Type resourceType;

    OverusedResource(double usage, double total, Type resourceType) {
      this.usage = usage;
      this.total = total;
      this.resourceType = resourceType;
    }

    public double getOverusageRatio() {
      return usage / total;
    }

    public static int prioritize(OverusedResource r1, OverusedResource r2) {
      if (r1.resourceType == r2.resourceType) {
        return Double.compare(r2.getOverusageRatio(), r1.getOverusageRatio());
      } else if (r1.resourceType == Type.MEMORY) {
        return -1;
      } else {
        return 1;
      }
    }

    public double match(double cpuUsage, double memUsageBytes) {
      if (resourceType == Type.CPU) {
        return cpuUsage;
      } else {
        return memUsageBytes;
      }
    }

    public boolean exceeds(double cpuUsage, double memUsageBytes) {
      return usage > match(cpuUsage, memUsageBytes);
    }

    public void updateOverusage(double cpuUsageDelta, double memUsageDelta) {
      usage -= match(cpuUsageDelta, memUsageDelta);
    }

    public TaskCleanupType toTaskCleanupType() {
      if (resourceType == Type.CPU) {
        return TaskCleanupType.REBALANCE_CPU_USAGE;
      } else {
        return TaskCleanupType.REBALANCE_MEMORY_USAGE;
      }
    }
  }

  static class OverusedSlave {
    SingularitySlaveUsage usage;
    List<TaskIdWithUsage> tasks;
    OverusedResource resource;

    OverusedSlave(SingularitySlaveUsage usage, List<TaskIdWithUsage> tasks, OverusedResource resource) {
      this.usage = usage;
      this.tasks = tasks;
      this.resource = resource;
    }
  }

  public void shuffle(Map<SingularitySlaveUsage, List<TaskIdWithUsage>> overloadedHosts) {
    LOG.debug("Beginning task shuffle for {} slaves", overloadedHosts.size());

    if (overloadedHosts.size() <= 0) {
      return;
    }

    List<OverusedSlave> slavesToShuffle = overloadedHosts.entrySet().stream()
        .map((entry) -> new OverusedSlave(entry.getKey(), entry.getValue(), getMostOverusedResource(entry.getKey())))
        .sorted((s1, s2) -> OverusedResource.prioritize(s1.resource, s2.resource))
        .collect(Collectors.toList());

    List<SingularityTaskCleanup> shufflingTasks = getShufflingTasks();
    Set<String> shufflingRequests = getAssociatedRequestIds(shufflingTasks);
    Map<String, Long> shufflingTasksPerHost = getShufflingTaskCountPerHost(shufflingTasks);

    long shufflingTasksOnCluster = shufflingTasks.size();
    LOG.debug("{} tasks currently shuffling on cluster", shufflingTasksOnCluster);

    for (OverusedSlave slave : slavesToShuffle) {
      if (shufflingTasksOnCluster >= configuration.getMaxTasksToShuffleTotal()) {
        LOG.debug("Not shuffling any more tasks (totalShuffleCleanups: {})", shufflingTasksOnCluster);
        break;
      }

      TaskCleanupType shuffleCleanupType = slave.resource.toTaskCleanupType();
      List<TaskIdWithUsage> shuffleCandidates = getPrioritizedShuffleCandidates(slave);

      long shufflingTasksOnSlave = shufflingTasksPerHost.getOrDefault(getHostId(slave).orElse(""), 0L);
      long availableTasksOnSlave = shuffleCandidates.size();
      double cpuUsage = getSystemCpuLoadForShuffle(slave.usage);
      double memUsageBytes = getSystemMemLoadForShuffle(slave.usage);

      for (TaskIdWithUsage task : shuffleCandidates) {
        availableTasksOnSlave--;

        if (shufflingRequests.contains(task.getTaskId().getRequestId())) {
          LOG.debug("Request {} already has a shuffling task, skipping", task.getTaskId().getRequestId());
          continue;
        }

        boolean resourceNotOverused = !isOverutilized(slave, cpuUsage, memUsageBytes);
        boolean tooManyShufflingTasks = isShufflingTooManyTasks(shufflingTasksOnSlave, shufflingTasksOnCluster);
        double taskCpuUsage = task.getUsage().getCpusUsed();
        double taskMemUsage = task.getUsage().getMemoryTotalBytes();

        if (resourceNotOverused || tooManyShufflingTasks) {
          LOG.debug("Not shuffling any more tasks on slave {} ({} overage : {}%, shuffledOnHost: {}, totalShuffleCleanups: {})",
              task.getTaskId().getSanitizedHost(),
              slave.resource.resourceType,
              slave.resource.getOverusageRatio() * 100,
              shufflingTasksOnSlave,
              shufflingTasksOnCluster
          );
          break;
        }

        long availableShufflesOnSlave = configuration.getMaxTasksToShufflePerHost() - shufflingTasksOnSlave;
        if (availableShufflesOnSlave == 1 && availableTasksOnSlave > 0 && slave.resource.exceeds(taskCpuUsage, taskMemUsage)) {
          LOG.debug("Skipping shuffling task {} on slave {} to reach threshold ({} overage : {}%, shuffledOnHost: {}, totalShuffleCleanups: {})",
              task.getTaskId().getId(),
              task.getTaskId().getSanitizedHost(),
              slave.resource.resourceType,
              slave.resource.getOverusageRatio() * 100,
              shufflingTasksOnSlave,
              shufflingTasksOnCluster
          );
          continue;
        }

        String message = getShuffleMessage(slave, task, cpuUsage, memUsageBytes);
        bounce(task, shuffleCleanupType, Optional.of(message));

        cpuUsage -= taskCpuUsage;
        memUsageBytes -= taskMemUsage;
        slave.resource.updateOverusage(taskCpuUsage, taskMemUsage);

        shufflingTasksOnSlave++;
        shufflingTasksOnCluster++;
        shufflingRequests.add(task.getTaskId().getRequestId());
      }
    }

    LOG.debug("Completed task shuffle for {} slaves", overloadedHosts.size());
  }

  private List<TaskIdWithUsage> getPrioritizedShuffleCandidates(OverusedSlave slave) {
    // SingularityUsageHelper ensures that requests flagged as always ineligible for shuffling have been filtered out.
    List<TaskIdWithUsage> out = slave.tasks;

    if (slave.resource.resourceType == Type.CPU) {
      out.sort((u1, u2) -> Double.compare(
          getUtilizationScoreForCPUShuffle(u2),
          getUtilizationScoreForCPUShuffle(u1)
      ));
    } else {
      out.sort((u1, u2) -> Double.compare(
          getUtilizationScoreForMemoryShuffle(u1),
          getUtilizationScoreForMemoryShuffle(u2)
      ));
    }

    return out;
  }

  private double getUtilizationScoreForCPUShuffle(TaskIdWithUsage task) {
    return task.getUsage().getCpusUsed() / task.getRequestedResources().getCpus();
  }

  private double getUtilizationScoreForMemoryShuffle(TaskIdWithUsage task) {
    double memoryUtilization = task.getUsage().getMemoryTotalBytes() / task.getRequestedResources().getMemoryMb();
    double cpuUtilization = task.getUsage().getCpusUsed() / task.getRequestedResources().getCpus();

    return (memoryUtilization + cpuUtilization) / 2;
  }

  private boolean isOverutilized(OverusedSlave slave, double cpuUsage, double memUsageBytes) {
    if (slave.resource.resourceType == Type.CPU) {
      return cpuUsage > slave.usage.getSystemCpusTotal();
    } else {
      return memUsageBytes > getTargetMemoryUtilizationForHost(slave.usage);
    }
  }

  private boolean isShufflingTooManyTasks(long shuffledOnSlave, long shuffledOnCluster) {
    return shuffledOnSlave >= configuration.getMaxTasksToShufflePerHost()
        || shuffledOnCluster >= configuration.getMaxTasksToShuffleTotal();
  }

  private void bounce(TaskIdWithUsage task, TaskCleanupType cleanupType, Optional<String> message) {
    String actionId = UUID.randomUUID().toString();

    taskManager.createTaskCleanup(new SingularityTaskCleanup(
        Optional.empty(),
        cleanupType,
        System.currentTimeMillis(),
        task.getTaskId(),
        message,
        Optional.of(actionId),
        Optional.empty(), Optional.empty()
    ));

    requestManager.addToPendingQueue(new SingularityPendingRequest(
        task.getTaskId().getRequestId(),
        task.getTaskId().getDeployId(),
        System.currentTimeMillis(),
        Optional.empty(),
        PendingType.TASK_BOUNCE,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        message,
        Optional.of(actionId)
    ));
  }

  private String getShuffleMessage(OverusedSlave slave, TaskIdWithUsage task, double cpuUsage, double memUsageBytes) {
    if (slave.resource.resourceType == Type.CPU) {
      return String.format(
          "Load on slave is %s / %s, shuffling task using %s / %s to less busy host",
          cpuUsage,
          slave.usage.getSystemCpusTotal(),
          task.getUsage().getCpusUsed(),
          task.getRequestedResources().getCpus()
      );
    } else {
      return String.format(
          "Mem usage on slave is %sMiB / %sMiB, shuffling task using %sMiB / %sMiB to less busy host",
          SizeUnit.BYTES.toMegabytes(((long) memUsageBytes)),
          SizeUnit.BYTES.toMegabytes(((long) slave.usage.getSystemMemTotalBytes())),
          SizeUnit.BYTES.toMegabytes(task.getUsage().getMemoryTotalBytes()),
          ((long) task.getRequestedResources().getMemoryMb())
      );
    }
  }

  private double getTargetMemoryUtilizationForHost(SingularitySlaveUsage usage) {
    return configuration.getShuffleTasksWhenSlaveMemoryUtilizationPercentageExceeds() * usage.getSystemMemTotalBytes();
  }

  private OverusedResource getMostOverusedResource(SingularitySlaveUsage overloadedSlave) {
    double currentCpuLoad = getSystemCpuLoadForShuffle(overloadedSlave);
    double currentMemUsageBytes = overloadedSlave.getMemoryBytesUsed();

    double targetCpuUsage = overloadedSlave.getSystemCpusTotal();
    double cpuOverage = currentCpuLoad - overloadedSlave.getSystemCpusTotal();
    double cpuOverusage = cpuOverage / overloadedSlave.getSystemCpusTotal();

    double targetMemUsageBytes = getTargetMemoryUtilizationForHost(overloadedSlave);
    double memOverageBytes = currentMemUsageBytes - targetMemUsageBytes;
    double memOverusage = memOverageBytes / targetMemUsageBytes;

    if (cpuOverusage > memOverusage) {
      return new OverusedResource(cpuOverage, targetCpuUsage, Type.CPU);
    } else {
      return new OverusedResource(memOverageBytes, targetMemUsageBytes, Type.MEMORY);
    }
  }

  private List<SingularityTaskCleanup> getShufflingTasks() {
    return taskManager.getCleanupTasks()
        .stream()
        .filter(SingularityTaskShuffler::isShuffleCleanup)
        .collect(Collectors.toList());
  }

  private Map<String, Long> getShufflingTaskCountPerHost(List<SingularityTaskCleanup> shufflingTasks) {
    Map<String, Long> out = new HashMap<>();

    for (SingularityTaskCleanup c : shufflingTasks) {
      String host = c.getTaskId().getSanitizedHost();
      out.replace(c.getTaskId().getSanitizedHost(), out.getOrDefault(host, 0L) + 1);
    }

    return out;
  }

  private Optional<String> getHostId(OverusedSlave slave) {
    if (slave.tasks.size() <= 0) {
      return Optional.empty();
    }

    // probably should add slave metadata to SingularitySlaveUsage
    return Optional.of(slave.tasks.get(0).getTaskId().getSanitizedHost());
  }

  private static boolean isShuffleCleanup(SingularityTaskCleanup cleanup) {
    TaskCleanupType type = cleanup.getCleanupType();
    return type == TaskCleanupType.REBALANCE_CPU_USAGE || type == TaskCleanupType.REBALANCE_MEMORY_USAGE;
  }

  private Set<String> getAssociatedRequestIds(List<SingularityTaskCleanup> cleanups) {
    return cleanups.stream()
        .map((taskCleanup) -> taskCleanup.getTaskId().getRequestId())
        .collect(Collectors.toSet());
  }

  private double getSystemCpuLoadForShuffle(SingularitySlaveUsage usage) {
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

  private double getSystemMemLoadForShuffle(SingularitySlaveUsage usage) {
    // usage.getMemoryBytesUsed() does not take external memory pressure into account
    return usage.getSystemMemTotalBytes() - usage.getSystemMemFreeBytes();
  }
}
