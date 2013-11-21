package com.hubspot.singularity.scheduler;

import java.util.List;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.data.TaskManager;

public class SingularitySchedulerBase {

  private final TaskManager taskManager;
  
  @Inject
  public SingularitySchedulerBase(TaskManager taskManager) {
    this.taskManager = taskManager;
  }

  protected List<SingularityTaskId> getMatchingActiveTaskIds(String requestId, List<SingularityTaskId> activeTaskIds, List<String> decomissioningRacks, List<SingularitySlave> decomissioningSlaves) {
    List<SingularityTaskId> matchingTaskIds = Lists.newArrayList();
    
    for (SingularityTaskId matchingTaskId : SingularityTaskId.filter(activeTaskIds, requestId)) {
      if (isOnActiveMachine(matchingTaskId, decomissioningRacks, decomissioningSlaves)) {
        matchingTaskIds.add(matchingTaskId);
      }
    }
    
    return matchingTaskIds;
  }  
  
  private boolean isOnActiveMachine(SingularityTaskId matchingTaskId, List<String> decomissioningRacks, List<SingularitySlave> decomissioningSlaves) {
    if (decomissioningRacks.contains(matchingTaskId.getRackId())) {
      return false;
    }
    
    for (SingularitySlave decomissioningSlave : decomissioningSlaves) {
      if (matchingTaskId.getHost().equals(decomissioningSlave.getHost())) {
        Optional<SingularityTask> task = taskManager.getActiveTask(matchingTaskId.getId());
        
        if (!task.isPresent()) {
          return false;
        }
        
        if (task.get().getMesosTask().getSlaveId().getValue().equals(decomissioningSlave.getId())) {
          return false;
        }
      }
    }
    
    return true;
  }
  
}
