package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    title = "A description of the current resource usage of a task",
    subTypes = {SingularityTaskCurrentUsage.class}
)
public class SingularityTaskCurrentUsageWithId extends SingularityTaskCurrentUsage {

  private final SingularityTaskId taskId;

  public SingularityTaskCurrentUsageWithId(SingularityTaskId taskId, SingularityTaskCurrentUsage taskCurrentUsage) {
    super(taskCurrentUsage.getMemoryTotalBytes(), taskCurrentUsage.getTimestamp(), taskCurrentUsage.getCpusUsed(), taskCurrentUsage.getDiskTotalBytes());

    this.taskId = taskId;
  }

  @JsonCreator
  public SingularityTaskCurrentUsageWithId(@JsonProperty("memoryTotalBytes") long memoryTotalBytes,
                                           @JsonProperty("long") long timestamp,
                                           @JsonProperty("cpusUsed") double cpusUsed,
                                           @JsonProperty("diskTotalBytes") long diskTotalBytes,
                                           @JsonProperty("taskId") SingularityTaskId taskId) {
    super(memoryTotalBytes, timestamp, cpusUsed, diskTotalBytes);
    this.taskId = taskId;
  }

  @Schema(description = "The ID of the task")
  public SingularityTaskId getTaskId() {
    return taskId;
  }

  @Override
  public String toString() {
    return "SingularityTaskCurrentUsageWithId [taskId=" + taskId + ", super=" + super.toString() + "]";
  }

}
