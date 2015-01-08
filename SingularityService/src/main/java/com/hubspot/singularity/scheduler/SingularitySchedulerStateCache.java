package com.hubspot.singularity.scheduler;

import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.data.RackManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;

public class SingularitySchedulerStateCache {

  private final TaskManager taskManager;

  private final SlaveManager slaveManager;
  private final RackManager rackManager;

  private final Map<String, Optional<SingularitySlave>> slaveCache;
  private final Map<String, Optional<SingularityRack>> rackCache;

  private Optional<List<SingularityTaskId>> activeTaskIds;
  private Optional<List<SingularityPendingTask>> scheduledTasks;
  private Optional<List<SingularityTaskId>> cleaningTasks;
  private Optional<Integer> numActiveRacks;
  private Optional<Integer> numActiveSlaves;

  @Inject
  public SingularitySchedulerStateCache(TaskManager taskManager, SlaveManager slaveManager, RackManager rackManager) {
    this.taskManager = taskManager;
    this.slaveManager = slaveManager;
    this.rackManager = rackManager;

    activeTaskIds = Optional.absent();
    scheduledTasks = Optional.absent();
    cleaningTasks = Optional.absent();
    numActiveRacks = Optional.absent();
    numActiveSlaves = Optional.absent();

    slaveCache = Maps.newHashMap();
    rackCache = Maps.newHashMap();
  }

  public List<SingularityTaskId> getActiveTaskIds() {
    if (!activeTaskIds.isPresent()) {
      activeTaskIds = getMutableList(taskManager.getActiveTaskIds());
    }

    return activeTaskIds.get();
  }

  public List<SingularityPendingTask> getScheduledTasks() {
    if (!scheduledTasks.isPresent()) {
      scheduledTasks = getMutableList(taskManager.getPendingTasks());
    }

    return scheduledTasks.get();
  }

  private <T> Optional<List<T>> getMutableList(List<T> immutableList) {
    List<T> mutableList = Lists.newArrayList(immutableList);
    return Optional.of(mutableList);
  }

  public List<SingularityTaskId> getCleaningTasks() {
    if (!cleaningTasks.isPresent()) {
      cleaningTasks = getMutableList(taskManager.getCleanupTaskIds());
    }

    return cleaningTasks.get();
  }

  public int getNumActiveRacks() {
    if (numActiveRacks.isPresent()) {
      return numActiveRacks.get();
    }

    numActiveRacks = Optional.of(rackManager.getNumObjectsAtState(MachineState.ACTIVE));

    return numActiveRacks.get();
  }

  public int getNumActiveSlaves() {
    if (numActiveSlaves.isPresent()) {
      return numActiveSlaves.get();
    }

    numActiveSlaves = Optional.of(slaveManager.getNumObjectsAtState(MachineState.ACTIVE));

    return numActiveSlaves.get();
  }

  public Optional<SingularitySlave> getSlave(String slaveId) {
    if (!slaveCache.containsKey(slaveId)) {
      slaveCache.put(slaveId, slaveManager.getObject(slaveId));
    }

    return slaveCache.get(slaveId);
  }

  public Optional<SingularityRack> getRack(String rackId) {
    if (!rackCache.containsKey(rackId)) {
      rackCache.put(rackId, rackManager.getObject(rackId));
    }

    return rackCache.get(rackId);
  }

}
