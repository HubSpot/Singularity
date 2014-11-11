package com.hubspot.singularity.scheduler;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;

@Singleton
public class SingularitySchedulerPriority {

  private static final Logger LOG = LoggerFactory.getLogger(SingularitySchedulerPriority.class);

  private final TaskManager taskManager;
  private final Map<String, Queue<SingularityTaskId>> mostRecentTasksPerRequest;
  private final Comparator<SingularityTaskRequest> comparator;

  @Inject
  public SingularitySchedulerPriority(TaskManager taskManager, RequestManager requestManager) {
    this.taskManager = taskManager;
    this.mostRecentTasksPerRequest = Maps.newHashMapWithExpectedSize(requestManager.getNumRequests());
    this.comparator = new Comparator<SingularityTaskRequest>() {

      @Override
      public int compare(SingularityTaskRequest o1, SingularityTaskRequest o2) {
        return 0;
      }
    };
  }

  public void sortTaskRequestsInPriorityOrder(final List<SingularityTaskRequest> taskRequests) {
    for (SingularityTaskRequest taskRequest : taskRequests) {
    }

    Collections.sort(taskRequests, comparator);
  }

  public void notifyTaskFinished(SingularityTaskId taskId) {

  }

  public void notifyTaskLaunched(SingularityTaskId taskId) {
  }

}
