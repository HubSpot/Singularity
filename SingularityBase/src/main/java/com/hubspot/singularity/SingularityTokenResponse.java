package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityTokenResponse {
  private final String token;
  private final SingularityUser user;

  @JsonCreator
  public SingularityTokenResponse(@JsonProperty("token") String token,
                                  @JsonProperty("user") SingularityUser user) {
    this.token = token;
    this.user = user;
  }

  public String getToken() {
    return token;
  }

  public SingularityUser getUser() {
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

    SingularityTokenResponse that = (SingularityTokenResponse) o;

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
    return "SingularityTokenResponse{" +
        "token='" + token + '\'' +
        ", user=" + user +
        '}';
  }
}
