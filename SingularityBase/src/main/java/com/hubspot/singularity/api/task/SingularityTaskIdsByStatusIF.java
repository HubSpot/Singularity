package com.hubspot.singularity.api.task;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "Tasks in all states for a particular request")
public interface SingularityTaskIdsByStatusIF {
  @Schema(description = "Active tasks whose healthchecks and load balancer updates (when applicable) have finished successfully")
  List<SingularityTaskId> getHealthy();

  @Schema(description = "Active tasks whose healthchecks and load balancer updates (when applicable) have not yet finished successfully")
  List<SingularityTaskId> getNotYetHealthy();

  @Schema(description = "Tasks that have not yet been launched")
  List<SingularityPendingTaskId> getPending();

  @Schema(description = "Active tasks in a cleaning state")
  List<SingularityTaskId> getCleaning();
}
