package com.hubspot.singularity.api.task;

import java.util.Optional;

import org.immutables.value.Value.Check;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import com.hubspot.singularity.annotations.SingularityStyleNoPublicConstructor;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyleNoPublicConstructor
@Schema(description = "Custom metadata associated with a Singularity task")
public abstract class AbstractSingularityTaskMetadata implements Comparable<SingularityTaskMetadata>, SingularityTaskIdHolder {
  public static final MetadataLevel DEFAULT_METADATA_LEVEL = MetadataLevel.INFO;

  @Schema(description = "Task id")
  public abstract SingularityTaskId getTaskId();

  @Schema(description = "Timestamp this metadata was created")
  public abstract long getTimestamp();

  @Schema(required = true, title = "Type of metadata", description = "Cannot contain a '/'")
  public abstract String getType();

  @Check
  protected void check() {
    Preconditions.checkState(!getType().contains("/"));
  }

  @Schema(description = "Title for this metadata")
  public abstract String getTitle();

  @Schema(description = "Optional metadata message", nullable = true)
  public abstract Optional<String> getMessage();

  @Schema(description = "The user who added this metadata", nullable = true)
  public abstract Optional<String> getUser();

  @Default
  @Schema(description = "Metadata level")
  public MetadataLevel getLevel() { return DEFAULT_METADATA_LEVEL; }

  @Override
  public int compareTo(SingularityTaskMetadata o) {
    return ComparisonChain.start()
        .compare(getTimestamp(), o.getTimestamp())
        .compare(getType(), o.getType())
        .compare(getLevel(), o.getLevel())
        .compare(getTaskId().getId(), o.getTaskId().getId())
        .result();
  }
}
