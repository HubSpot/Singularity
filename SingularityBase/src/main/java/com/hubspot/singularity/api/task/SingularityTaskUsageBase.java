package com.hubspot.singularity.api.task;

import io.swagger.v3.oas.annotations.media.Schema;

public interface SingularityTaskUsageBase {
  @Schema(description = "The total memory used by the task in bytes")
  long getMemoryTotalBytes();

  @Schema(description = "The time at which this usage data was collected")
  long getTimestamp();

  @Schema(description = "The cpus used by this task")
  double getCpusUsed();

  @Schema(description = "The total disk usage for this task in bytes")
  long getDiskTotalBytes();
}
