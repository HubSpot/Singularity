package com.hubspot.singularity;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to create a new long lived auth token")
public class SingularityTokenRequest {
  private final Optional<String> token;
  private final Optional<SingularityUser> user;

  @JsonCreator
  public SingularityTokenRequest(@JsonProperty("token") Optional<String> token,
                                 @JsonProperty("user") Optional<SingularityUser> user) {
    this.token = token;
    this.user = user;
  }

  @Schema(description = "Optional token, will be auto-genearted if not specified", required = false)
  public Optional<String> getToken() {
    return token;
  }

  @Schema(description = "User data associated with the token, will be the current logged in user if not provided", required = true)
  public Optional<SingularityUser> getUser() {
    return user;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SingularityTokenRequest that = (SingularityTokenRequest) o;

    if (token != null ? !token.equals(that.token) : that.token != null) {
      return false;
    }
    return user != null ? user.equals(that.user) : that.user == null;
  }

  @Override
  public int hashCode() {
    int result = token != null ? token.hashCode() : 0;
    result = 31 * result + (user != null ? user.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "SingularityTokenRequest{" +
        "token=" + token +
        ", user=" + user +
        '}';
  }
}
