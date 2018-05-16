package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Describes the attributes of a Singularity user")
public class SingularityUserHolder {
  private final Optional<SingularityUser> user;
  private final Optional<SingularityUserSettings> settings;
  private final boolean authenticated;
  private final boolean authEnabled;

  @JsonCreator
  public SingularityUserHolder(@JsonProperty("user") Optional<SingularityUser> user,
                               @JsonProperty("settings") Optional<SingularityUserSettings> settings,
                               @JsonProperty("authenticated") boolean authenticated,
                               @JsonProperty("authEnabled") boolean authEnabled) {
    this.user = user;
    this.settings = settings;
    this.authenticated = authenticated;
    this.authEnabled = authEnabled;
  }

  @Schema(description = "Information identifying this particular user")
  public Optional<SingularityUser> getUser() {
    return user;
  }

  @Schema(description = "Settings for this particular user")
  public Optional<SingularityUserSettings> getSettings() {
    return settings;
  }

  @Schema(description = "true if the user is authenticated")
  public boolean isAuthenticated() {
    return authenticated;
  }

  @Schema(description = "true if authentication is enabled")
  public boolean isAuthEnabled() {
    return authEnabled;
  }

  @Override
  public String toString() {
    return "SingularityUserHolder{" +
        "user=" + user +
        ", settings=" + settings +
        ", authenticated=" + authenticated +
        ", authEnabled=" + authEnabled +
        '}';
  }
}
