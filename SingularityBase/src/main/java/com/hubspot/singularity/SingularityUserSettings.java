package com.hubspot.singularity;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityUserSettings {
  private final Set<String> starredRequestIds;

  @JsonCreator
  public SingularityUserSettings(
      @JsonProperty("starredRequestIds") Set<String> starredRequestIds) {
    this.starredRequestIds = starredRequestIds != null ? starredRequestIds : Collections.<String>emptySet();
  }

  public static SingularityUserSettings empty() {
    return new SingularityUserSettings(Collections.<String>emptySet());
  }

  public Set<String> getStarredRequestIds() {
    return starredRequestIds;
  }

  public SingularityUserSettings addStarredRequestIds(Set<String> newStarredRequestIds) {
    starredRequestIds.addAll(newStarredRequestIds);
    return this;
  }

  public SingularityUserSettings deleteStarredRequestIds(Set<String> oldStarredRequestIds) {
    starredRequestIds.removeAll(oldStarredRequestIds);
    return this;
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

  @Override
  public String toString() {
    return "SingularityUserSettings{" +
        "starredRequestIds=" + starredRequestIds +
        '}';
  }
}
