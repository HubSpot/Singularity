package com.hubspot.singularity.api.auth;

import java.security.Principal;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "Information about a user")
public abstract class AbstractSingularityUser implements Principal {
  public static SingularityUser DEFAULT_USER = SingularityUser.builder().setId("singularity").build();

  @Schema(description = "The user's id")
  public abstract String getId();

  @Default
  @Nullable
  @Schema(description = "The user's name, or id if name not specified")
  public String getName() {
    return getId();
  }

  @Schema(description = "The user's email", nullable = true)
  public abstract Optional<String> getEmail();

  @Schema(description = "Groups this user is a part of")
  public abstract Set<String> getGroups();

  @Default
  @Schema(description = "True if the user was successfully authenticated")
  public boolean isAuthenticated() {
    return false;
  }
}
