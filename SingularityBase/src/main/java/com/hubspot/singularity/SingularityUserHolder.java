package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

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

  public Optional<SingularityUser> getUser() {
    return user;
  }

  public Optional<SingularityUserSettings> getSettings() {
    return settings;
  }

  public boolean isAuthenticated() {
    return authenticated;
  }

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
