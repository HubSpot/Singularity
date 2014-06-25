package com.hubspot.singularity.auth;

public class SingularityUser {
  private final String username;

  public SingularityUser(String username) {
    this.username = username;
  }

  public String getUsername() {
    return username;
  }

  @Override
  public String toString() {
    return "SingularityUser [" +
        "username='" + username + '\'' +
        ']';
  }

  public static final SingularityUser ANONYMOUS = new SingularityUser("");
}
