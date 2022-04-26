package com.hubspot.singularity.data.zkmigrations;

import com.google.inject.Inject;
import com.hubspot.singularity.data.TaskManager;
import javax.inject.Singleton;

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
