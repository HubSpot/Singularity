package com.hubspot.singularity.data.zkmigrations;

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
import org.apache.mesos.Protos.TaskID;
import org.apache.mesos.Protos.TaskStatus;

import javax.inject.Singleton;
import java.util.List;

@Singleton
public class TaskManagerRequiredParentsForTransactionsMigration extends ZkDataMigration {

  private final TaskManager taskManager;

  @Inject
  public TaskManagerRequiredParentsForTransactionsMigration(TaskManager taskManager) {
    super(5);
    this.taskManager = taskManager;
  }

  @Override
  public void applyMigration() {
    taskManager.createRequiredParents();
  }

}
