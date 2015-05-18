package com.hubspot.singularity;

import java.util.Set;

public class SingularityUser {
  private final String username;
  private final Set<String> groups;

  public SingularityUser(String username, Set<String> groups) {
    this.username = username;
    this.groups = groups;
  }

  public String getUsername() {
    return username;
  }

  public Set<String> getGroups() {
    return groups;
  }
}
