package com.hubspot.singularity.scheduler;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Longs;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.data.PriorityManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;

@Singleton
public class SingularitySchedulerPriority {

  private final TaskManager taskManager;
  private final Map<String, Long> mostRecentTaskStartPerRequest;
  private final Comparator<SingularityTaskRequest> comparator;

  private static final Comparator<SingularityTaskId> TASK_ID_STARTED_AT_COMPARATOR = new Comparator<SingularityTaskId>() {

    @Override
    public int compare(SingularityTaskId o1, SingularityTaskId o2) {
      return Longs.compare(o2.getStartedAt(), o1.getStartedAt());
    }

  };

  @Inject
  public SingularitySchedulerPriority(TaskManager taskManager, final PriorityManager priorityManager) {
    this.taskManager = taskManager;
    this.mostRecentTaskStartPerRequest = Maps.newHashMap();
    this.comparator = new Comparator<SingularityTaskRequest>() {

      @Override
      public int compare(SingularityTaskRequest o1, SingularityTaskRequest o2) {
        final Double request1Priority = priorityManager.getTaskPriorityLevelForRequest(o1.getRequest());
        final Double request2Priority = priorityManager.getTaskPriorityLevelForRequest(o2.getRequest());
        int priorityComp = Doubles.compare(request1Priority, request2Priority);

        if (priorityComp != 0) {
          return priorityComp;
        } else {
          final Long request1LastStartedAt = mostRecentTaskStartPerRequest.get(o1.getRequest().getId());
          final Long request2LastStartedAt = mostRecentTaskStartPerRequest.get(o2.getRequest().getId());
          return Longs.compare(request1LastStartedAt, request2LastStartedAt);
        }
      }
    };
  }

  public void sortTaskRequestsInPriorityOrder(final List<SingularityTaskRequest> taskRequests) {
    for (SingularityTaskRequest taskRequest : taskRequests) {
      if (!mostRecentTaskStartPerRequest.containsKey(taskRequest.getRequest().getId())) {
        List<SingularityTaskId> taskIds = Lists.newArrayList(taskManager.getTaskIdsForRequest(taskRequest.getRequest().getId()));

        if (!taskIds.isEmpty()) {
          Collections.sort(taskIds, TASK_ID_STARTED_AT_COMPARATOR);
          mostRecentTaskStartPerRequest.put(taskRequest.getRequest().getId(), JavaUtils.getFirst(taskIds).get().getStartedAt());
        } else {
          mostRecentTaskStartPerRequest.put(taskRequest.getRequest().getId(), 0L);
        }
      }
    }

    Collections.sort(taskRequests, comparator);
  }

  public void notifyTaskLaunched(SingularityTaskId taskId) {
    mostRecentTaskStartPerRequest.put(taskId.getRequestId(), taskId.getStartedAt());
  }

}
