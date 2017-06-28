package com.hubspot.singularity;

public class SingularityTaskCurrentUsageWithId extends SingularityTaskCurrentUsage {

  private final SingularityTaskId taskId;

  public SingularityTaskCurrentUsageWithId(SingularityTaskId taskId, SingularityTaskCurrentUsage taskCurrentUsage) {
    super(taskCurrentUsage.getMemoryTotalBytes(), taskCurrentUsage.getTimestamp(), taskCurrentUsage.getCpusUsed());

    this.taskId = taskId;
  }

  public SingularityTaskId getTaskId() {
    return taskId;
  }

  @Override
  public String toString() {
    return "SingularityTaskCurrentUsageWithId [taskId=" + taskId + ", super=" + super.toString() + "]";
  }

}
