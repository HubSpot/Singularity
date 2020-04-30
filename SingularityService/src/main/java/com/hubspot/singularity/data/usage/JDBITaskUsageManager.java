package com.hubspot.singularity.data.usage;

import com.google.inject.Inject;
import com.hubspot.singularity.InvalidSingularityTaskIdException;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.config.SingularityConfiguration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JDBITaskUsageManager implements TaskUsageManager {
  private static final Logger LOG = LoggerFactory.getLogger(JDBITaskUsageManager.class);

  private final TaskUsageJDBI taskUsageJDBI;
  private final SingularityConfiguration configuration;

  @Inject
  public JDBITaskUsageManager(
    TaskUsageJDBI taskUsageJDBI,
    SingularityConfiguration configuration
  ) {
    this.taskUsageJDBI = taskUsageJDBI;
    this.configuration = configuration;
  }

  public void deleteTaskUsage(SingularityTaskId taskId) {
    taskUsageJDBI.deleteTaskUsage(taskId.getId());
  }

  public void deleteSpecificTaskUsage(SingularityTaskId taskId, long timestamp) {
    taskUsageJDBI.deleteSpecificTaskUsage(taskId.getId(), timestamp);
  }

  public void saveSpecificTaskUsage(
    SingularityTaskId taskId,
    SingularityTaskUsage usage
  ) {
    taskUsageJDBI.saveSpecificTaskUsage(
      taskId.getRequestId(),
      taskId.getId(),
      usage.getMemoryTotalBytes(),
      usage.getTimestamp(),
      usage.getCpuSeconds(),
      usage.getDiskTotalBytes(),
      usage.getCpusNrPeriods(),
      usage.getCpusNrThrottled(),
      usage.getCpusThrottledTimeSecs()
    );
  }

  public List<SingularityTaskUsage> getTaskUsage(SingularityTaskId taskId) {
    return taskUsageJDBI.getTaskUsage(taskId.getId());
  }

  public int countTasksWithUsage() {
    return taskUsageJDBI.countTasksWithUsage();
  }

  public void cleanOldUsages(List<SingularityTaskId> activeTaskIds) {
    for (String taskIdString : taskUsageJDBI.getUniqueTaskIds()) {
      try {
        SingularityTaskId taskId = SingularityTaskId.valueOf(taskIdString);
        if (activeTaskIds.contains(taskId)) {
          taskUsageJDBI
            .getUsageTimestampsForTask(taskIdString)
            .stream()
            .sorted((t1, t2) -> Long.compare(t2, t1))
            .skip(configuration.getNumUsageToKeep())
            .forEach(timestamp -> deleteSpecificTaskUsage(taskId, timestamp));
          continue;
        }
      } catch (InvalidSingularityTaskIdException e) {
        LOG.warn(
          "{} is not a valid task id, will remove task usage from zookeeper",
          taskIdString
        );
      }
      taskUsageJDBI.deleteTaskUsage(taskIdString);

      LOG.debug("Deleted obsolete task usage {}", taskIdString);
    }
  }
}
