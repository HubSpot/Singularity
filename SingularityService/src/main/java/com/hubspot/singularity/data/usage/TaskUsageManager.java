package com.hubspot.singularity.data.usage;

import java.util.Comparator;
import java.util.List;

import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskUsage;

public interface TaskUsageManager {
  Comparator<SingularityTaskUsage> TASK_USAGE_COMPARATOR_TIMESTAMP_ASC = Comparator.comparingDouble(SingularityTaskUsage::getTimestamp);

  void deleteTaskUsage(SingularityTaskId taskId);

  void deleteSpecificTaskUsage(SingularityTaskId taskId, long timestamp);

  void saveSpecificTaskUsage(SingularityTaskId taskId, SingularityTaskUsage usage);

  List<SingularityTaskUsage> getTaskUsage(SingularityTaskId taskId);

  int countTasksWithUsage();

  void cleanOldUsages(List<SingularityTaskId> activeTaskIds);
}
