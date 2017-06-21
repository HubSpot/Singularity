package com.hubspot.singularity.data.zkmigrations;

import java.util.List;

import javax.inject.Singleton;

import org.apache.mesos.Protos.TaskID;
import org.apache.mesos.Protos.TaskStatus;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskStatusHolder;
import com.hubspot.singularity.data.TaskManager;

@Singleton
public class LastTaskStatusMigration extends ZkDataMigration {

  private final TaskManager taskManager;
  private final String serverId;

  @Inject
  public LastTaskStatusMigration(TaskManager taskManager, @Named(SingularityMainModule.SERVER_ID_PROPERTY) String serverId) {
    super(1);
    this.taskManager = taskManager;
    this.serverId = serverId;
  }

  @Override
  public void applyMigration() {
    final long start = System.currentTimeMillis();
    final List<SingularityTaskId> taskIds = taskManager.getActiveTaskIds();

    for (SingularityTaskId taskId : taskIds) {
      List<SingularityTaskHistoryUpdate> updates = Lists.reverse(taskManager.getTaskHistoryUpdates(taskId));
      Optional<TaskStatus> taskStatus = Optional.absent();

      for (SingularityTaskHistoryUpdate update : updates) {
        if (update.getTaskState().toTaskState().isPresent()) {
          Optional<SingularityTask> task = taskManager.getTask(taskId);

          taskStatus = Optional.of(TaskStatus.newBuilder()
              .setTaskId(TaskID.newBuilder().setValue(taskId.getId()))
              .setSlaveId(task.get().getSlaveId())
              .setState(update.getTaskState().toTaskState().get())
              .build());

          break;
        }
      }

      SingularityTaskStatusHolder taskStatusHolder = new SingularityTaskStatusHolder(taskId, taskStatus, start, serverId, Optional.<String> absent());

      taskManager.saveLastActiveTaskStatus(taskStatusHolder);
    }
  }


}
