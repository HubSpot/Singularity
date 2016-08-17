package com.hubspot.singularity;

import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class SingularityUserSettings {
  private final String userId;
  private final Set<String> starredRequestIds;

  @JsonCreator
  public SingularityUserSettings(
      @JsonProperty("userId") String userId,
      @JsonProperty("starredRequestIds") Set<String> starredRequestIds) {
    this.userId = userId;
    this.starredRequestIds = Objects.firstNonNull(starredRequestIds, Collections.<String>emptySet());
  }

  public String getUserId() {
    return userId;
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
  public String toString() {
    return "SingularityUserSettings[" +
        "userId=" + userId +
        ", starredRequestIds=" + starredRequestIds +
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

    if (!Objects.equal(userId, that.userId)) {
      return false;
    }
    if (!Objects.equal(starredRequestIds, that.starredRequestIds)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(userId, starredRequestIds);
  }
}
