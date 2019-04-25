package com.hubspot.singularity.data.usage;

import java.util.List;
import java.util.Map;

import com.hubspot.singularity.SingularityTaskCurrentUsageWithId;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskUsage;

public class JDBITaskUsageManager implements TaskUsageManager {

  public void deleteTaskUsage(String taskId) {

  }

  public void deleteSpecificTaskUsage(String taskId, double timestamp) {

  }

  public void saveCurrentTaskUsage(SingularityTaskCurrentUsageWithId usageWithId) {

  }

  public void saveSpecificTaskUsage(String taskId, SingularityTaskUsage usage) {

  }

  public List<SingularityTaskUsage> getTaskUsage(String taskId) {

  }

  public Map<String, SingularityTaskCurrentUsageWithId> getTaskCurrentUsages(List<SingularityTaskId> taskIds) {

  }
}
