package com.hubspot.mesos.client;

public class UserAndPassword {

  private final String user;
  private final String password;

  public static UserAndPassword empty() {
    return new UserAndPassword(null, null);
  }

  public UserAndPassword(String user, String password) {
    this.user = user;
    this.password = password;
  }

  public String getUser() {
    return user;
  }

  public String getPassword() {
    return password;
  }

  public boolean hasCredentials() {
    return user != null && password != null;
  }
}
