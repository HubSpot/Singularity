package com.hubspot.singularity;

import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityUserSettings {
  private final Set<String> starredRequestIds;

  @JsonCreator
  public SingularityUserSettings(@JsonProperty Set<String> starredRequestIds) {
    this.starredRequestIds = starredRequestIds;
  }

  public Set<String> getStarredRequestIds() {
    return starredRequestIds;
  }

  @Override
  public String toString() {
    return "SingularityUserSettings[" +
        "starredRequestIds=" + starredRequestIds +
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
    SingularityUserSettings that = (SingularityUserSettings) o;
    return Objects.equals(starredRequestIds, that.starredRequestIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(starredRequestIds);
  }
}
