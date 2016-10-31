package com.hubspot.singularity;

import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class SingularityUserSettings {
  private final Set<String> starredRequestIds;

  @JsonCreator
  public SingularityUserSettings(
      @JsonProperty("starredRequestIds") Set<String> starredRequestIds) {
    this.starredRequestIds = Objects.firstNonNull(starredRequestIds, Collections.<String>emptySet());
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
    return Objects.equal(starredRequestIds, that.starredRequestIds);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(starredRequestIds);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("starredRequestIds", starredRequestIds)
      .toString();
  }
}
