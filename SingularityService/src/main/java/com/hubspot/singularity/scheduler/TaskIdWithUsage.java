package com.hubspot.singularity.scheduler;

import com.hubspot.mesos.Resources;
import com.hubspot.singularity.SingularityTaskCurrentUsage;
import com.hubspot.singularity.SingularityTaskId;

class TaskIdWithUsage {
  private final SingularityTaskId taskId;
  private final Resources requestedResources;
  private final SingularityTaskCurrentUsage usage;

  TaskIdWithUsage(SingularityTaskId taskId, Resources requestedResources, SingularityTaskCurrentUsage usage) {
    this.taskId = taskId;
    this.requestedResources = requestedResources;
    this.usage = usage;
  }

  public SingularityTaskId getTaskId() {
    return taskId;
  }

  public Resources getRequestedResources() {
    return requestedResources;
  }

  public SingularityTaskCurrentUsage getUsage() {
    return usage;
  }
}
