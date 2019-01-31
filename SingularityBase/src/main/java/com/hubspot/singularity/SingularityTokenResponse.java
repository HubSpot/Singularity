package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response containing the generated long lived auth token and associated user data")
public class SingularityTokenResponse {
  private final String token;
  private final SingularityUser user;

  @JsonCreator
  public SingularityTokenResponse(@JsonProperty("token") String token,
                                  @JsonProperty("user") SingularityUser user) {
    this.token = token;
    this.user = user;
  }

  @Schema(description = "The generated/saved token", required = true)
  public String getToken() {
    return token;
  }

  @Schema(description = "User data associated with the token", required = true)
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
