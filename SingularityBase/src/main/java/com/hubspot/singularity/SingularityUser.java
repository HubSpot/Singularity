package com.hubspot.singularity;

import java.util.Objects;
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

  @Override
  public String toString() {
    return "SingularityUser[" +
            "username='" + username + '\'' +
            ", groups=" + groups +
            ']';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityUser that = (SingularityUser) o;
    return Objects.equals(username, that.username) &&
            Objects.equals(groups, that.groups);
  }

  @Override
  public int hashCode() {
    return Objects.hash(username, groups);
  }
}
