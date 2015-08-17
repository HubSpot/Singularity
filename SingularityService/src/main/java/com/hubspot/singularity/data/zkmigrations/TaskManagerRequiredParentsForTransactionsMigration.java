package com.hubspot.singularity.data.zkmigrations;

import javax.inject.Singleton;

import com.google.inject.Inject;
import com.hubspot.singularity.data.TaskManager;

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
