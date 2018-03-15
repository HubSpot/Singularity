package com.hubspot.singularity.api.request;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "A request to update a pending deploy")
public interface SingularityUpdatePendingDeployRequestIF {
  @Schema(description = "Request id", required=true)
  String getRequestId();

  @Schema(description = "Deploy id", required=true)
  String getDeployId();

  @Schema(description = "Updated target instance count for the active deploy", required=true)
  int getTargetActiveInstances();
}
