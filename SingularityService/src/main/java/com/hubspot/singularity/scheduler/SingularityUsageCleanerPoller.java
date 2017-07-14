package com.hubspot.singularity.scheduler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.UsageManager;

public class SingularityUsageCleanerPoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityUsageCleanerPoller.class);

  private final SingularityUsageHelper usageHelper;
  private final UsageManager usageManager;
  private final TaskManager taskManager;

  @Inject
  SingularityUsageCleanerPoller(SingularityConfiguration configuration, SingularityUsageHelper usageHelper, UsageManager usageManager, TaskManager taskManager) {
    super(configuration.getCleanUsageEveryMillis(), TimeUnit.MILLISECONDS);

    this.usageHelper = usageHelper;
    this.usageManager = usageManager;
    this.taskManager = taskManager;
  }

  @Override
  public void runActionOnPoll() {
    deleteObsoleteSlaveUsage();
    deleteObsoleteTaskUsage();
  }

  private void deleteObsoleteSlaveUsage() {
    Set<String> slaveIdsToTrackUsageFor = usageHelper.getSlaveIdsToTrackUsageFor();

    for (String slaveIdWithUsage : usageManager.getSlavesWithUsage()) {
      if (slaveIdsToTrackUsageFor.contains(slaveIdWithUsage)) {
        continue;
      }

      SingularityDeleteResult result = usageManager.deleteSlaveUsage(slaveIdWithUsage);

      LOG.debug("Deleted obsolete slave usage {} - {}", slaveIdWithUsage, result);
    }
  }

  private void deleteObsoleteTaskUsage() {
    Set<String> taskIds = new HashSet<>(taskManager.getActiveTaskIdsAsStrings());
    List<String> taskIdsWithUsage = usageManager.getTasksWithUsage();

    for (String taskIdWithUsage : taskIdsWithUsage) {
      if (taskIds.contains(taskIdWithUsage)) {
        continue;
      }

      SingularityDeleteResult result = usageManager.deleteTaskUsage(taskIdWithUsage);

      LOG.debug("Deleted obsolete task usage {} - {}", taskIdWithUsage, result);
    }
  }
}
