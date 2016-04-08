package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityUserHolder {
  private final Optional<SingularityUser> user;
  private final boolean authenticated;
  private final boolean authEnabled;
  private final boolean isAdmin;

  @JsonCreator
  public SingularityUserHolder(@JsonProperty("user") Optional<SingularityUser> user,
                               @JsonProperty("authenticated") boolean authenticated,
                               @JsonProperty("authEnabled") boolean authEnabled,
                               @JsonProperty("isAdmin") boolean isAdmin) {
    this.user = user;
    this.authenticated = authenticated;
    this.authEnabled = authEnabled;
    this.isAdmin = isAdmin;
  }

  public Optional<SingularityUser> getUser() {
    return user;
  }

  public boolean isAuthenticated() {
    return authenticated;
  }

  public boolean isAuthEnabled() {
    return authEnabled;
  }

  public boolean isAdmin() { return isAdmin; }
}
