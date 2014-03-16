package com.hubspot.singularity.scheduler;

import java.util.List;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.data.RackManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;

public class SingularityScheduleStateCache {

  private final TaskManager taskManager;
  
  private final SlaveManager slaveManager;
  private final RackManager rackManager;
  
  private Optional<List<SingularityTaskId>> activeTaskIds;
  private Optional<List<SingularityPendingTask>> scheduledTasks;
  private Optional<List<SingularityRack>> decomissioningRacks;
  private Optional<Set<String>> decomissioningRackIds;
  private Optional<List<SingularitySlave>> decomissioningSlaves;
  private Optional<Set<String>> decomissioningSlaveIds;
  private Optional<List<SingularityTaskId>> cleaningTasks;
  
  @Inject
  public SingularityScheduleStateCache(TaskManager taskManager, SlaveManager slaveManager, RackManager rackManager) {
    this.taskManager = taskManager;
    this.slaveManager = slaveManager;
    this.rackManager = rackManager;
    
    activeTaskIds = Optional.absent();
    scheduledTasks = Optional.absent();
    decomissioningRacks = Optional.absent();
    decomissioningRackIds = Optional.absent();
    decomissioningSlaves = Optional.absent();
    decomissioningSlaveIds = Optional.absent();
    cleaningTasks = Optional.absent();
  }
  
  public List<SingularityTaskId> getActiveTaskIds() {
    if (!activeTaskIds.isPresent()) {
      activeTaskIds = Optional.of(taskManager.getActiveTaskIds());
    }
    
    return activeTaskIds.get();
  }

  public List<SingularityPendingTask> getScheduledTasks() {
    if (!scheduledTasks.isPresent()) {
      scheduledTasks = Optional.of(taskManager.getScheduledTasks());
    }
    
    return scheduledTasks.get();
  }
  
  private void checkDecomissioningRacks() {
    if (decomissioningRacks.isPresent()) {
      return;
    }
     
    decomissioningRacks = Optional.of(rackManager.getDecomissioningObjects());
    Set<String> decomissioningRacksIdsSet = Sets.newHashSetWithExpectedSize(decomissioningRacks.get().size());
    for (SingularityRack rack : decomissioningRacks.get()) {
      decomissioningRacksIdsSet.add(rack.getId());
    }
    decomissioningRackIds = Optional.of(decomissioningRacksIdsSet);
  }
  
  public List<SingularityRack> getDecomissioningRacks() {
    checkDecomissioningRacks();
    
    return decomissioningRacks.get();
  }
  
  private void checkDecomissioningSlaves() {
    if (decomissioningSlaves.isPresent()) {
      return;
    }
    
    decomissioningSlaves = Optional.of(slaveManager.getDecomissioningObjects());
    Set<String> decomissioningSlaveIdsSet = Sets.newHashSetWithExpectedSize(decomissioningSlaves.get().size());
    for (SingularitySlave slave : decomissioningSlaves.get()) {
      decomissioningSlaveIdsSet.add(slave.getId());
    }
    decomissioningSlaveIds = Optional.of(decomissioningSlaveIdsSet);
  }
  
  public List<SingularitySlave> getDecomissioningSlaves() {
    checkDecomissioningSlaves();
    
    return decomissioningSlaves.get();
  }
  
  public List<SingularityTaskId> getCleaningTasks() {
    if (!cleaningTasks.isPresent()) {
      cleaningTasks = Optional.of(taskManager.getCleanupTaskIds());
    }
    
    return cleaningTasks.get();   
  }
  
  public boolean isSlaveDecomissioning(String slaveId) {
    checkDecomissioningSlaves();
    
    return decomissioningSlaveIds.get().contains(slaveId);
  }
  
  public boolean isRackDecomissioning(String rackId) {
    checkDecomissioningRacks();
    
    return decomissioningRackIds.get().contains(rackId);
  }

}
