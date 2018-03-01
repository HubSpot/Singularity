package com.hubspot.singularity.api.task;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "Current usage for a task")
public interface SingularityTaskUsageIF {
  @Schema(description = "Current total memory usage in bytes")
  long getMemoryTotalBytes();

  @Schema(description = "Timestamp this usage was recorded (epoch seconds)")
  double getTimestamp();

  @Schema(description = "Seconds of cpu time consumed by this task")
  double getCpuSeconds();

  @Schema(description = "Total disk usage in bytes for this task")
  long getDiskTotalBytes();
}
