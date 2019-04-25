package com.hubspot.singularity.data.usage;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.hubspot.singularity.SingularityTaskCurrentUsageWithId;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskUsage;

public interface TaskUsageManager {
  Comparator<SingularityTaskUsage> TASK_USAGE_COMPARATOR_TIMESTAMP_ASC = Comparator.comparingDouble(SingularityTaskUsage::getTimestamp);
  Comparator<String> TASK_USAGE_PATH_COMPARATOR_TIMESTAMP_ASC = Comparator.comparingDouble(Double::parseDouble);

  void deleteTaskUsage(String taskId);

  void deleteSpecificTaskUsage(String taskId, double timestamp);

  void saveCurrentTaskUsage(SingularityTaskCurrentUsageWithId usageWithId);

  void saveSpecificTaskUsage(String taskId, SingularityTaskUsage usage);

  List<SingularityTaskUsage> getTaskUsage(String taskId);

  Map<String, SingularityTaskCurrentUsageWithId> getTaskCurrentUsages(List<SingularityTaskId> taskIds);
}
