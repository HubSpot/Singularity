package com.hubspot.singularity.helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.data.RackManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.scheduler.SingularityLeaderCache;

@Singleton
public class RebalancingHelper {
  private static final Logger LOG = LoggerFactory.getLogger(RebalancingHelper.class);

  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final SlaveManager slaveManager;
  private final RackManager rackManager;
  private final SingularityLeaderCache leaderCache;

  @Inject
  public RebalancingHelper(TaskManager taskManager, RequestManager requestManager, SlaveManager slaveManager,
      RackManager rackManager, SingularityLeaderCache leaderCache) {
    this.taskManager = taskManager;
    this.requestManager = requestManager;
    this.slaveManager = slaveManager;
    this.rackManager = rackManager;
    this.leaderCache = leaderCache;
  }

  public List<SingularityTaskId> rebalanceRacks(SingularityRequest request, List<SingularityTaskId> remainingActiveTasks, Optional<String> user) {
    List<SingularityTaskId> extraCleanedTasks = new ArrayList<>();
    int numActiveRacks = rackManager.getNumActive();
    double perRack = request.getInstancesSafe() / (double) numActiveRacks;

    Multiset<String> countPerRack = HashMultiset.create();
    for (SingularityTaskId taskId : remainingActiveTasks) {
      countPerRack.add(taskId.getRackId());
      LOG.info("{} - {} - {} - {}", countPerRack, perRack, extraCleanedTasks.size(), taskId);
      if (countPerRack.count(taskId.getRackId()) > perRack && extraCleanedTasks.size() < numActiveRacks / 2) {
        extraCleanedTasks.add(taskId);
        LOG.info("Cleaning up task {} to evenly distribute tasks among racks", taskId);
        taskManager.createTaskCleanup(new SingularityTaskCleanup(user, TaskCleanupType.REBALANCE_RACKS, System.currentTimeMillis(),
            taskId, Optional.absent(), Optional.absent(), Optional.absent()));
      }
    }
    return extraCleanedTasks;
  }

  public Set<SingularityTaskId> rebalanceAttributeDistribution(
      SingularityRequest request,
      Optional<String> user,
      List<SingularityTaskId> remainingActiveTasks) {

    Map<String, Map<String, Set<SingularityTaskId>>> attributeTaskMap = new HashMap<>();

    for (SingularityTaskId taskId : remainingActiveTasks) {
      SingularitySlave slave = leaderCache.getSlave(taskManager.getTask(taskId).get().getMesosTask().getSlaveId().getValue()).get();
      for (Entry<String, String> entry : slave.getAttributes().entrySet()) {
        attributeTaskMap
            .computeIfAbsent(entry.getKey(), key -> new HashMap<>())
            .computeIfAbsent(entry.getValue(), key -> new HashSet<>())
            .add(taskId);
      }
    }

    Integer numDesiredInstances = request.getInstancesSafe();
    Set<SingularityTaskId> extraTasksToClean = new HashSet<>();

    for (Entry<String, Map<String, Integer>> keyEntry : request.getSlaveAttributeMinimums().get().entrySet()) {
      for (Entry<String, Integer> valueEntry : keyEntry.getValue().entrySet()) {
        String attributeName = keyEntry.getKey();
        String attributeValue = valueEntry.getKey();
        Integer attributePercent = valueEntry.getValue();
        Set<SingularityTaskId> matchingTaskIds = attributeTaskMap.get(attributeName).get(attributeValue);
        matchingTaskIds.removeAll(extraTasksToClean);

        int minInstancesWithAttr = Math.max(1, (int) Math.ceil((attributePercent / 100.0) * numDesiredInstances));
        int numInstancesWithAttr = attributeTaskMap.containsKey(attributeName) && attributeTaskMap.get(attributeName).containsKey(attributeValue)
            ? matchingTaskIds.size()
            : 0;
        int maxPotentialInstancesWithAttr = numInstancesWithAttr + (numDesiredInstances - remainingActiveTasks.size());

        int numTasksToClean = minInstancesWithAttr - maxPotentialInstancesWithAttr;
        if (numTasksToClean > 0) {
          Set<SingularityTaskId> tasksToClean = remainingActiveTasks.stream().filter(t -> !matchingTaskIds.contains(t)).limit(numTasksToClean).collect(Collectors.toSet());
          LOG.info("Marking tasks {} for cleanup to satisfy attribute {}={} on at least {}% of instances", tasksToClean, attributeName, attributeValue, valueEntry.getValue());
          extraTasksToClean.addAll(tasksToClean);
        }
      }
    }

    for (SingularityTaskId taskId : extraTasksToClean) {
      taskManager.createTaskCleanup(
          new SingularityTaskCleanup(user, TaskCleanupType.REBALANCE_SLAVE_ATTRIBUTES, System.currentTimeMillis(), taskId, Optional.absent(), Optional.absent(), Optional.absent()));
    }
    return extraTasksToClean;
  }
}
