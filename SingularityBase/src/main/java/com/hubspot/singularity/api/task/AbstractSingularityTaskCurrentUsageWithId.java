package com.hubspot.singularity.api.task;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(
    title = "A description of the current resource usage of a task",
    subTypes = {SingularityTaskCurrentUsage.class}
)
public abstract class AbstractSingularityTaskCurrentUsageWithId implements SingularityTaskUsageBase {
  @Schema(description = "The total memory used by the task in bytes")
  public abstract long getMemoryTotalBytes();

  @Schema(description = "The time at which this usage data was collected")
  public abstract long getTimestamp();

  @Schema(description = "The cpus used by this task")
  public abstract double getCpusUsed();

  @Schema(description = "The total disk usage for this task in bytes")
  public abstract long getDiskTotalBytes();

  @Schema(description = "The ID of the task")
  public abstract SingularityTaskId getTaskId();
}
