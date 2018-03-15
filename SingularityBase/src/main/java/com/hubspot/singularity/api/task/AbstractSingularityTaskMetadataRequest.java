package com.hubspot.singularity.api.task;

import java.util.Optional;

import org.immutables.value.Value.Check;
import org.immutables.value.Value.Immutable;

import com.google.common.base.Preconditions;
import com.hubspot.singularity.annotations.SingularityStyleNoPublicConstructor;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyleNoPublicConstructor
@Schema(description = "Request to add custom metadata to a task")
public abstract class AbstractSingularityTaskMetadataRequest {
  @Schema(required = true, description = "A type to be associated with this metadata")
  public abstract String getType();

  @Check
  protected void check() {
    Preconditions.checkState(!getType().contains("/"));
  }

  @Schema(required = true, description = "A title to be associated with this metadata")
  public abstract String getTitle();

  @Schema(description = "An optional message")
  public abstract Optional<String> getMessage();

  @Schema(description = "Level of metadata, can be INFO, WARN, or ERROR", nullable = true)
  public abstract Optional<MetadataLevel> getLevel();
}
