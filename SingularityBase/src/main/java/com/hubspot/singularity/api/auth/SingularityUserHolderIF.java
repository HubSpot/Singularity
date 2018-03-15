package com.hubspot.singularity.api.auth;

import java.util.Optional;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "Describes the attributes of a Singularity user")
public interface SingularityUserHolderIF {
  @Schema(description = "Information identifying this particular user")
  Optional<SingularityUser> getUser();

  @Schema(description = "Settings for this particular user")
  Optional<SingularityUserSettings> getSettings();

  @Schema(description = "true if the user is authenticated")
  boolean isAuthenticated();

  @Schema(description = "true if authentication is enabled")
  boolean isAuthEnabled();
}
