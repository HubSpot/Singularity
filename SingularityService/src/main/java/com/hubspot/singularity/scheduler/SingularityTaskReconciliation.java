package com.hubspot.singularity.scheduler;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.mesos.Protos.TaskStatus;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.data.TaskManager;

public class SingularityTaskReconciliation {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityTaskReconciliation.class);

  private final TaskManager taskManager;

  @Inject
  public SingularityTaskReconciliation(TaskManager taskManager) {
    this.taskManager = taskManager;
  }

  public void reconcileTasks(SchedulerDriver driver) {
    Set<SingularityTaskId> taskIds = Sets.newHashSet(taskManager.getActiveTaskIds());
    List<TaskStatus> taskStatuses = taskManager.getLastActiveTaskStatuses();

    for (Iterator<TaskStatus> taskStatusItr = taskStatuses.iterator(); taskStatusItr.hasNext(); ) {
      TaskStatus taskStatus = taskStatusItr.next();
      SingularityTaskId taskId = SingularityTaskId.fromString(taskStatus.getTaskId().toString());

      if (!taskIds.contains(taskId)) {
        LOG.info("Task {} not found, deleting taskStatus", taskId);
        taskManager.deleteLastActiveTaskStatus(taskId);
        taskStatusItr.remove();
      }
    }

    driver.reconcileTasks(taskStatuses);
  }

}
