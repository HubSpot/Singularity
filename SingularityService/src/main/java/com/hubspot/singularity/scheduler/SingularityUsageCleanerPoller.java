package com.hubspot.singularity.scheduler;

import com.google.inject.Inject;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.usage.UsageManager;
import java.util.concurrent.TimeUnit;

public class SingularityUsageCleanerPoller extends SingularityLeaderOnlyPoller {

  private final UsageManager usageManager;
  private final TaskManager taskManager;

  @Inject
  SingularityUsageCleanerPoller(
    SingularityConfiguration configuration,
    UsageManager usageManager,
    TaskManager taskManager
  ) {
    super(configuration.getCleanUsageEveryMillis(), TimeUnit.MILLISECONDS);
    this.usageManager = usageManager;
    this.taskManager = taskManager;
  }

  @Override
  public void runActionOnPoll() {
    usageManager.cleanOldUsages(taskManager.getActiveTaskIds());
  }

  @Override
  protected boolean abortsOnError() {
    return false;
  }
}
