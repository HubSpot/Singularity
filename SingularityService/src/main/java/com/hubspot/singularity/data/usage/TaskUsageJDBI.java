package com.hubspot.singularity.data.usage;

import java.util.List;

import com.hubspot.singularity.SingularityTaskUsage;

public abstract class TaskUsageJDBI {
  static final String FIELDS = "requestId, taskId, memoryTotalBytes, timestamp, cpuSeconds, diskTotalBytes, cpusNrPeriods, cpusNrThrottled, cpusThrottledTimeSecs";
  static final String FIELD_VALUES = ":requestId, :taskId, :memoryTotalBytes, :timestamp, :cpuSeconds, :diskTotalBytes, :cpusNrPeriods, :cpusNrThrottled, :cpusThrottledTimeSecs";

  public abstract void deleteTaskUsage(String taskId);

  public abstract void deleteSpecificTaskUsage(String taskId, long timestamp);

  public abstract void saveSpecificTaskUsage(String requestId, String taskId, long memoryTotalBytes, long timestamp, double cpuSeconds, long diskTotalBytes, long cpusNrPeriods, long cpusNrThrottled,
                                             double cpusThrottledTimeSecs);

  public abstract List<SingularityTaskUsage> getTaskUsage(String taskId);

  public abstract List<String> getUniqueTaskIds();

  public abstract List<Long> getUsageTimestampsForTask(String taskId);

  public abstract int countTasksWithUsage();
}
