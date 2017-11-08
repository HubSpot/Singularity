package com.hubspot.singularity.data.zkmigrations;

import java.util.List;

import javax.inject.Singleton;

import org.apache.mesos.v1.Protos.TaskID;
import org.apache.mesos.v1.Protos.TaskStatus;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.protos.MesosTaskStatusObject;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskStatusHolder;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.helpers.MesosProtosUtils;

@Singleton
public class LastTaskStatusMigration extends ZkDataMigration {

  private final TaskManager taskManager;
  private final String serverId;
  private final MesosProtosUtils mesosProtosUtils;

  @Inject
  public LastTaskStatusMigration(TaskManager taskManager, @Named(SingularityMainModule.SERVER_ID_PROPERTY) String serverId, MesosProtosUtils mesosProtosUtils) {
    super(1);
    this.taskManager = taskManager;
    this.serverId = serverId;
    this.mesosProtosUtils = mesosProtosUtils;
  }

  @Override
  public void applyMigration() {
    final long start = System.currentTimeMillis();
    final List<SingularityTaskId> taskIds = taskManager.getActiveTaskIds();

    for (SingularityTaskId taskId : taskIds) {
      List<SingularityTaskHistoryUpdate> updates = Lists.reverse(taskManager.getTaskHistoryUpdates(taskId));
      Optional<MesosTaskStatusObject> taskStatus = Optional.absent();

      for (SingularityTaskHistoryUpdate update : updates) {
        if (update.getTaskState().toTaskState().isPresent()) {
          Optional<SingularityTask> task = taskManager.getTask(taskId);

          taskStatus = Optional.of(mesosProtosUtils.taskStatusFromProtos(TaskStatus.newBuilder()
              .setTaskId(TaskID.newBuilder().setValue(taskId.getId()))
              .setAgentId(MesosProtosUtils.toAgentId(task.get().getAgentId()))
              .setState(MesosProtosUtils.toTaskState(update.getTaskState()))
              .build()));

          break;
        }
      }

      SingularityTaskStatusHolder taskStatusHolder = new SingularityTaskStatusHolder(taskId, taskStatus, start, serverId, Optional.absent());

      taskManager.saveLastActiveTaskStatus(taskStatusHolder);
    }
  }


}
