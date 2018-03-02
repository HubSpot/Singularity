package com.hubspot.singularity.api.request;

import java.util.Optional;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "Settings for how an unpause should behave")
public interface SingularityUnpauseRequestIF {
  @Schema(description = "A message to show to users about why this action was taken", nullable = true)
  Optional<String> getMessage();

  @Schema(description = "An id to associate with this action for metadata purposes", nullable = true)
  Optional<String> getActionId();

  @Schema(description = "If set to true, instructs new tasks that are scheduled immediately while unpausing to skip healthchecks", nullable = true)
  Optional<Boolean> getSkipHealthchecks();
}
