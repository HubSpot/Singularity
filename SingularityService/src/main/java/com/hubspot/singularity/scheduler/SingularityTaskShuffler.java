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

    ;

    double overusage;
    Type resourceType;

    OverusedResource(double overusage, Type resourceType) {
      this.overusage = overusage;
      this.resourceType = resourceType;
    }

    public static int prioritize(OverusedResource r1, OverusedResource r2) {
      if (r1.resourceType == r2.resourceType) {
        return Double.compare(r2.overusage, r1.overusage);
      } else if (r1.resourceType == Type.MEMORY) {
        return -1;
      } else {
        return 1;
      }
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
    List<OverusedSlave> slavesToShuffle = overloadedHosts.entrySet().stream()
        .map((entry) -> new OverusedSlave(entry.getKey(), entry.getValue(), getMostOverusedResource(entry.getKey())))
        .sorted((s1, s2) -> OverusedResource.prioritize(s1.resource, s2.resource))
        .collect(Collectors.toList());

    List<SingularityTaskCleanup> shufflingTasks = getShufflingTasks();
    Set<String> shufflingRequests = getAssociatedRequestIds(shufflingTasks);
    long shufflingTasksOnCluster = shufflingTasks.size();

    for (OverusedSlave slave : slavesToShuffle) {
      if (shufflingTasksOnCluster >= configuration.getMaxTasksToShuffleTotal()) {
        LOG.debug("Not shuffling any more tasks (totalShuffleCleanups: {})", shufflingTasksOnCluster);
        break;
      }

      long shufflingTasksOnSlave = 0;
      double cpuUsage = getSystemLoadForShuffle(slave.usage);
      double memUsageBytes = slave.usage.getMemoryBytesUsed();

      TaskCleanupType shuffleCleanupType = slave.resource.toTaskCleanupType();
      List<TaskIdWithUsage> shuffleCandidates = getPrioritizedShuffleCandidates(slave);

      for (TaskIdWithUsage task : shuffleCandidates) {
        if (shufflingRequests.contains(task.getTaskId().getRequestId())) {
          LOG.debug("Request {} already has a shuffling task, skipping", task.getTaskId().getRequestId());
          continue;
        }

        boolean resourceNotOverused = !isOverutilized(slave, cpuUsage, memUsageBytes);
        boolean tooManyShufflingTasks = isShufflingTooManyTasks(shufflingTasksOnSlave, shufflingTasksOnCluster);

        if (resourceNotOverused || tooManyShufflingTasks) {
          LOG.debug("Not shuffling any more tasks on slave {} ({} overage : {}%, shuffledOnHost: {}, totalShuffleCleanups: {})",
              task.getTaskId().getSanitizedHost(),
              slave.resource.resourceType,
              slave.resource.overusage * 100,
              shufflingTasksOnSlave,
              shufflingTasksOnCluster
          );
          break;
        }

        String message = getShuffleMessage(slave, task, cpuUsage, memUsageBytes);
        bounce(task, shuffleCleanupType, Optional.of(message));

        cpuUsage -= task.getUsage().getCpusUsed();
        memUsageBytes -= task.getUsage().getMemoryTotalBytes();

        shufflingTasksOnSlave++;
        shufflingTasksOnCluster++;
        shufflingRequests.add(task.getTaskId().getRequestId());
      }
    }
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
    return shuffledOnSlave > configuration.getMaxTasksToShufflePerHost()
        || shuffledOnCluster > configuration.getMaxTasksToShuffleTotal();
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
    double currentCpuLoad = getSystemLoadForShuffle(overloadedSlave);
    double currentMemUsageBytes = overloadedSlave.getMemoryBytesUsed();

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

  private List<SingularityTaskCleanup> getShufflingTasks() {
    return taskManager.getCleanupTasks()
        .stream()
        .filter(SingularityTaskShuffler::isShuffleCleanup)
        .collect(Collectors.toList());
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
}
