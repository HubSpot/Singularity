package com.hubspot.singularity.auth.authenticator;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityUser;

public class SingularityUserPermissionsResponse {
  private final Optional<SingularityUser> user;
  private final boolean authenticated;
  private final Optional<String> error;

  @JsonCreator
  public SingularityUserPermissionsResponse(@JsonProperty("user") Optional<SingularityUser> user, @JsonProperty("authenticated") boolean authenticated, @JsonProperty("error") Optional<String> error) {
    this.user = user;
    this.authenticated = authenticated;
    this.error = error;
  }

  public Optional<SingularityUser> getUser() {
    return user;
  }

  public boolean isAuthenticated() {
    return authenticated;
  }

  public Optional<String> getError() {
    return error;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof SingularityUserPermissionsResponse) {
      final SingularityUserPermissionsResponse that = (SingularityUserPermissionsResponse) obj;
      return Objects.equals(this.authenticated, that.authenticated) &&
          Objects.equals(this.user, that.user) &&
          Objects.equals(this.error, that.error);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(user, authenticated, error);
  }

  @Override
  public String toString() {
    return "SingularityUserPermissionsResponse{" +
        "user=" + user +
        ", authenticated=" + authenticated +
        ", error=" + error +
        '}';
  }
}
