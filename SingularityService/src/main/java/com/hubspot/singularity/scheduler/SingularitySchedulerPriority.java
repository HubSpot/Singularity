package com.hubspot.singularity.scheduler;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.primitives.Doubles;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.PriorityManager;

@Singleton
public class SingularitySchedulerPriority {
  private final Comparator<SingularityTaskRequest> comparator;

  @Inject
  public SingularitySchedulerPriority(final PriorityManager priorityManager, final SingularityConfiguration configuration) {
    this.comparator = new Comparator<SingularityTaskRequest>() {

      @Override
      public int compare(SingularityTaskRequest o1, SingularityTaskRequest o2) {
        final Long pendingTask1OverdueTimeMillis = Math.max(System.currentTimeMillis() - o1.getPendingTask().getPendingTaskId().getNextRunAt(), 1);
        final Long pendingTask2OverdueTimeMillis = Math.max(System.currentTimeMillis() - o2.getPendingTask().getPendingTaskId().getNextRunAt(), 1);

        final Double pendingTask1Priority = priorityManager.getTaskPriorityLevelForRequest(o1.getRequest());
        final Double pendingTask2Priority = priorityManager.getTaskPriorityLevelForRequest(o2.getRequest());

        final Double pendingTask1WeightedPriority = pendingTask1OverdueTimeMillis * Math.pow(pendingTask1Priority, configuration.getSchedulerPriorityWeightFactor());
        final Double pendingTask2WeightedPriority = pendingTask2OverdueTimeMillis * Math.pow(pendingTask2Priority, configuration.getSchedulerPriorityWeightFactor());

        return Doubles.compare(pendingTask2WeightedPriority, pendingTask1WeightedPriority);
      }
    };
  }

  public void sortTaskRequestsInPriorityOrder(final List<SingularityTaskRequest> taskRequests) {
    Collections.sort(taskRequests, comparator);
  }
}
