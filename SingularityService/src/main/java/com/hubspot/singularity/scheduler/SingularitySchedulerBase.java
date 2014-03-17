package com.hubspot.singularity.scheduler;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTaskId;

public class SingularitySchedulerBase {

  @Inject
  public SingularitySchedulerBase() {}

  protected List<SingularityTaskId> getMatchingActiveTaskIds(String requestId, List<SingularityTaskId> activeTaskIds, List<SingularityTaskId> cleaningTasks) {
    List<SingularityTaskId> matchingTaskIds = Lists.newArrayList();
    
    for (SingularityTaskId matchingTaskId : SingularityTaskId.filter(activeTaskIds, requestId)) {
      if (!cleaningTasks.contains(matchingTaskId)) {
        matchingTaskIds.add(matchingTaskId);
      }
    }
    
    return matchingTaskIds;
  }
  
}
