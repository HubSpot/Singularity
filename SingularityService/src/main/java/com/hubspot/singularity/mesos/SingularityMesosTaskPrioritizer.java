package com.hubspot.singularity.mesos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityPriorityFreezeParent;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SingularityTaskRequestWithPriority;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.PriorityManager;

@Singleton
public class SingularityMesosTaskPrioritizer {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMesosTaskPrioritizer.class);

  private final SingularityConfiguration configuration;
  private final PriorityManager priorityManager;

  @Inject
  public SingularityMesosTaskPrioritizer(SingularityConfiguration configuration, PriorityManager priorityManager) {
    this.configuration = configuration;
    this.priorityManager = priorityManager;
  }

  public List<SingularityTaskRequest> getSortedDueTasks(List<SingularityTaskRequest> dueTasks) {
    long now = System.currentTimeMillis();
    List<SingularityTaskRequestWithPriority> taskRequestWithPriorities = new ArrayList<>();
    for (SingularityTaskRequest taskRequest : dueTasks) {
      taskRequestWithPriorities.add(new SingularityTaskRequestWithPriority(taskRequest, getWeightedPriority(taskRequest, now)));
    }
    Collections.sort(taskRequestWithPriorities, SingularityTaskRequestWithPriority.weightedPriorityComparator());
    List<SingularityTaskRequest> taskRequests = new ArrayList<>();
    for (SingularityTaskRequestWithPriority taskRequestWithPriority : taskRequestWithPriorities) {
      taskRequests.add(taskRequestWithPriority.getTaskRequest());
    }
    return taskRequests;
  }

  public void removeTasksAffectedByPriorityFreeze(List<SingularityTaskRequest> taskRequests) {
    final Optional<SingularityPriorityFreezeParent> maybePriorityFreeze = priorityManager.getActivePriorityFreeze();

    if (maybePriorityFreeze.isPresent()) {
      final ListIterator<SingularityTaskRequest> iterator = taskRequests.listIterator();

      while (iterator.hasNext()) {
        final SingularityTaskRequest taskRequest = iterator.next();

        final double taskPriorityLevel = priorityManager.getTaskPriorityLevelForRequest(taskRequest.getRequest());

        if (taskPriorityLevel < maybePriorityFreeze.get().getPriorityFreeze().getMinimumPriorityLevel()) {
          LOG.trace("Skipping scheduled task {} because taskPriorityLevel ({}) is less than active priority freeze ({})", taskRequest.getPendingTask().getPendingTaskId(), taskPriorityLevel,
              maybePriorityFreeze.get().getPriorityFreeze().getMinimumPriorityLevel());
          iterator.remove();
        }
      }
    }
  }

  private double getWeightedPriority(SingularityTaskRequest taskRequest, long now) {
    Long overdueMillis = Math.max(now - taskRequest.getPendingTask().getPendingTaskId().getNextRunAt(), 1);
    Double requestPriority = priorityManager.getTaskPriorityLevelForRequest(taskRequest.getRequest());
    return overdueMillis * Math.pow(requestPriority, configuration.getSchedulerPriorityWeightFactor());
  }

}
