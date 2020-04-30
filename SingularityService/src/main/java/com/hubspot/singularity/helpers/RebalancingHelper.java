package com.hubspot.singularity.helpers;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.mesos.SingularitySlaveAndRackManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class RebalancingHelper {
  private static final Logger LOG = LoggerFactory.getLogger(RebalancingHelper.class);

  private final TaskManager taskManager;
  private final SlaveManager slaveManager;
  private final SingularitySlaveAndRackManager slaveAndRackManager;

  @Inject
  public RebalancingHelper(
    TaskManager taskManager,
    SlaveManager slaveManager,
    SingularitySlaveAndRackManager slaveAndRackManager
  ) {
    this.taskManager = taskManager;
    this.slaveManager = slaveManager;
    this.slaveAndRackManager = slaveAndRackManager;
  }

  public List<SingularityTaskId> rebalanceRacks(
    SingularityRequest request,
    List<SingularityTaskId> remainingActiveTasks,
    Optional<String> user
  ) {
    List<SingularityTaskId> extraCleanedTasks = new ArrayList<>();
    int activeRacksWithCapacityCount = slaveAndRackManager.getActiveRacksWithCapacityCount();
    double perRack = request.getInstancesSafe() / (double) activeRacksWithCapacityCount;

    Multiset<String> countPerRack = HashMultiset.create();
    for (SingularityTaskId taskId : remainingActiveTasks) {
      countPerRack.add(taskId.getRackId());
      LOG.info(
        "{} - {} - {} - {}",
        countPerRack,
        perRack,
        extraCleanedTasks.size(),
        taskId
      );
      if (
        countPerRack.count(taskId.getRackId()) > perRack &&
        extraCleanedTasks.size() < activeRacksWithCapacityCount / 2 &&
        taskId.getInstanceNo() > 1
      ) {
        extraCleanedTasks.add(taskId);
        LOG.info("Cleaning up task {} to evenly distribute tasks among racks", taskId);
        taskManager.createTaskCleanup(
          new SingularityTaskCleanup(
            user,
            TaskCleanupType.REBALANCE_RACKS,
            System.currentTimeMillis(),
            taskId,
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
          )
        );
      }
    }
    return extraCleanedTasks;
  }

  public Set<SingularityTaskId> rebalanceAttributeDistribution(
    SingularityRequest request,
    Optional<String> user,
    List<SingularityTaskId> remainingActiveTasks
  ) {
    Map<String, Map<String, Set<SingularityTaskId>>> attributeTaskMap = new HashMap<>();

    for (SingularityTaskId taskId : remainingActiveTasks) {
      SingularitySlave slave = slaveManager
        .getObject(
          taskManager.getTask(taskId).get().getMesosTask().getSlaveId().getValue()
        )
        .get();
      for (Entry<String, String> entry : slave.getAttributes().entrySet()) {
        attributeTaskMap
          .computeIfAbsent(entry.getKey(), key -> new HashMap<>())
          .computeIfAbsent(entry.getValue(), key -> new HashSet<>())
          .add(taskId);
      }
    }

    Integer numDesiredInstances = request.getInstancesSafe();
    Set<SingularityTaskId> extraTasksToClean = new HashSet<>();

    for (Entry<String, Map<String, Integer>> keyEntry : request
      .getSlaveAttributeMinimums()
      .get()
      .entrySet()) {
      for (Entry<String, Integer> valueEntry : keyEntry.getValue().entrySet()) {
        String attributeName = keyEntry.getKey();
        String attributeValue = valueEntry.getKey();
        Integer attributePercent = valueEntry.getValue();
        Set<SingularityTaskId> matchingTaskIds = attributeTaskMap
          .get(attributeName)
          .get(attributeValue);
        matchingTaskIds.removeAll(extraTasksToClean);

        int minInstancesWithAttr = Math.max(
          1,
          (int) Math.ceil((attributePercent / 100.0) * numDesiredInstances)
        );
        int numInstancesWithAttr = attributeTaskMap.containsKey(attributeName) &&
          attributeTaskMap.get(attributeName).containsKey(attributeValue)
          ? matchingTaskIds.size()
          : 0;
        int maxPotentialInstancesWithAttr =
          numInstancesWithAttr + (numDesiredInstances - remainingActiveTasks.size());

        int numTasksToClean = minInstancesWithAttr - maxPotentialInstancesWithAttr;
        if (numTasksToClean > 0) {
          Set<SingularityTaskId> tasksToClean = remainingActiveTasks
            .stream()
            .filter(t -> !matchingTaskIds.contains(t))
            .limit(numTasksToClean)
            .collect(Collectors.toSet());
          LOG.info(
            "Marking tasks {} for cleanup to satisfy attribute {}={} on at least {}% of instances",
            tasksToClean,
            attributeName,
            attributeValue,
            valueEntry.getValue()
          );
          extraTasksToClean.addAll(tasksToClean);
        }
      }
    }

    for (SingularityTaskId taskId : extraTasksToClean) {
      taskManager.createTaskCleanup(
        new SingularityTaskCleanup(
          user,
          TaskCleanupType.REBALANCE_SLAVE_ATTRIBUTES,
          System.currentTimeMillis(),
          taskId,
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
        )
      );
    }
    return extraTasksToClean;
  }
}
