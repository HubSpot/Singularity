package com.hubspot.singularity.api.auth;

import java.util.Set;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "Describes the settings for a particular user")
public interface SingularityUserSettingsIF {
  @Schema(description = "The list of starred request ids for a particular user")
  Set<String> getStarredRequestIds();
}
