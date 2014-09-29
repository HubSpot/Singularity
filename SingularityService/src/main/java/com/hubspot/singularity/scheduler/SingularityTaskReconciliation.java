package com.hubspot.singularity.scheduler;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.mesos.Protos.Status;
import org.apache.mesos.Protos.TaskID;
import org.apache.mesos.Protos.TaskState;
import org.apache.mesos.Protos.TaskStatus;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskHistoryUpdate.SimplifiedTaskState;
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
    final long start = System.currentTimeMillis();

    Set<SingularityTaskId> taskIds = Sets.newHashSet(taskManager.getActiveTaskIds());
    List<TaskStatus> taskStatuses = taskManager.getLastActiveTaskStatuses();

    for (Iterator<TaskStatus> taskStatusItr = taskStatuses.iterator(); taskStatusItr.hasNext();) {
      TaskStatus taskStatus = taskStatusItr.next();
      SingularityTaskId taskId = SingularityTaskId.fromString(taskStatus.getTaskId().getValue());

      if (!taskIds.contains(taskId)) {
        LOG.info("Task {} not found, deleting taskStatus", taskId);
        taskManager.deleteLastActiveTaskStatus(taskId);
        taskStatusItr.remove();
      } else {
        taskIds.remove(taskId);
      }
    }

    // didn't find a taskStatus -- this is temporary for migration
    for (SingularityTaskId taskId : taskIds) {
      Optional<SingularityTask> task = taskManager.getActiveTask(taskId.getId());

      if (!task.isPresent()) {
        LOG.info("Task {} didn't have an active task", taskId);
        continue;
      }

      List<SingularityTaskHistoryUpdate> updates = taskManager.getTaskHistoryUpdates(taskId);

      if (updates.isEmpty()) {
        LOG.info("Task {} didn't have updates", taskId);
        continue;
      }

      SimplifiedTaskState simplifiedTaskState = SingularityTaskHistoryUpdate.getCurrentState(updates);

      TaskState taskState = TaskState.TASK_RUNNING;

      if (simplifiedTaskState == SimplifiedTaskState.UNKNOWN) {
        taskState = TaskState.TASK_STARTING;
      }

      LOG.info("Adding a task status {} for {}", taskState, taskId);

      taskStatuses.add(TaskStatus.newBuilder()
          .setSlaveId(task.get().getOffer().getSlaveId())
          .setState(taskState)
          .setTaskId(TaskID.newBuilder().setValue(taskId.getId()))
          .build());
    }

    LOG.info("Requesting reconciliation for {} taskStatuses", taskStatuses.size());

    Status status = driver.reconcileTasks(taskStatuses);

    LOG.info("Requested reconciliation of {} taskStatuses with driver status {} in {}", taskStatuses.size(), status, JavaUtils.duration(start));
  }

}
