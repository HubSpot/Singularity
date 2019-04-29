package com.hubspot.singularity.data.usage;

import java.util.Date;
import java.util.List;

import com.hubspot.singularity.SingularityTaskUsage;

public abstract class TaskUsageJDBI {
  static final String FILEDS = "requestId, taskId, memoryTotalBytes, timestamp, cpuSeconds, diskTotalBytes, cpusNrPeriods, cpusNrThrottled, cpusThrottledTimeSecs";
  static final String FILED_VALUES = ":requestId, :taskId, :memoryTotalBytes, :timestamp, :cpuSeconds, :diskTotalBytes, :cpusNrPeriods, :cpusNrThrottled, :cpusThrottledTimeSecs";

  public abstract void deleteTaskUsage(String taskId);

  public abstract void deleteSpecificTaskUsage(String taskId, Date timestamp);

  public abstract void saveSpecificTaskUsage(String requestId, String taskId, long memoryTotalBytes, Date timestamp, double cpuSeconds, long diskTotalBytes, long cpusNrPeriods, long cpusNrThrottled,
                                             double cpusThrottledTimeSecs);

  public abstract List<SingularityTaskUsage> getTaskUsage(String taskId);

  public abstract List<String> getUniqueTaskIds();

  public abstract int countTasksWithUsage();
}
