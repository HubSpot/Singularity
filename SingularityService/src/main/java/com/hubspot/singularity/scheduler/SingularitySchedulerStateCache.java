package com.hubspot.singularity.scheduler;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityKilledTaskIdRecord;
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

  private Optional<Collection<SingularityTaskId>> activeTaskIds;
  private Optional<Collection<SingularityPendingTask>> scheduledTasks;
  private Optional<Collection<SingularityTaskId>> cleaningTasks;
  private Optional<Collection<SingularityTaskId>> killedTasks;
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
    killedTasks = Optional.absent();
    numActiveRacks = Optional.absent();
    numActiveSlaves = Optional.absent();

    slaveCache = Maps.newHashMap();
    rackCache = Maps.newHashMap();
  }

  public Collection<SingularityTaskId> getActiveTaskIds() {
    if (!activeTaskIds.isPresent()) {
      activeTaskIds = getMutableCollection(taskManager.getActiveTaskIds());
    }

    return activeTaskIds.get();
  }

  public Collection<SingularityPendingTask> getScheduledTasks() {
    if (!scheduledTasks.isPresent()) {
      scheduledTasks = getMutableCollection(taskManager.getPendingTasks());
    }

    return scheduledTasks.get();
  }

  private <T> Optional<Collection<T>> getMutableCollection(List<T> immutableList) {
    Collection<T> mutableSet = Sets.newHashSet(immutableList);
    return Optional.of(mutableSet);
  }

  public Collection<SingularityTaskId> getCleaningTasks() {
    if (!cleaningTasks.isPresent()) {
      cleaningTasks = getMutableCollection(taskManager.getCleanupTaskIds());
    }

    return cleaningTasks.get();
  }

  public Collection<SingularityTaskId> getKilledTasks() {
    if (!killedTasks.isPresent()) {
      List<SingularityKilledTaskIdRecord> killedTaskRecords = taskManager.getKilledTaskIdRecords();
      Collection<SingularityTaskId> taskIds = Sets.newHashSet();
      for (SingularityKilledTaskIdRecord record : killedTaskRecords) {
        taskIds.add(record.getTaskId());
      }
      killedTasks = Optional.of(taskIds);
    }
    return killedTasks.get();
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
