package com.hubspot.singularity.data.usage;

import com.hubspot.singularity.SingularityTaskUsage;
import java.util.List;

public interface TaskUsageJDBI {
  String FIELDS =
    "requestId, taskId, memoryTotalBytes, timestamp, cpuSeconds, diskTotalBytes, cpusNrPeriods, cpusNrThrottled, cpusThrottledTimeSecs";
  String FIELD_VALUES =
    ":requestId, :taskId, :memoryTotalBytes, :timestamp, :cpuSeconds, :diskTotalBytes, :cpusNrPeriods, :cpusNrThrottled, :cpusThrottledTimeSecs";

  void deleteTaskUsage(String taskId);

  void deleteSpecificTaskUsage(String taskId, long timestamp);

  void saveSpecificTaskUsage(
    String requestId,
    String taskId,
    long memoryTotalBytes,
    long timestamp,
    double cpuSeconds,
    long diskTotalBytes,
    long cpusNrPeriods,
    long cpusNrThrottled,
    double cpusThrottledTimeSecs
  );

  List<SingularityTaskUsage> getTaskUsage(String taskId);

  List<String> getUniqueTaskIds();

  List<Long> getUsageTimestampsForTask(String taskId);

  int countTasksWithUsage();
}
