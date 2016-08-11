package com.hubspot.singularity;

import static com.google.common.collect.ImmutableSet.copyOf;

import java.util.Set;

public class SingularityUserSettings {
  private final Set<String> starredRequestIds;

  public SingularityUserSettings(Set<String> starredRequestIds) {
    this.starredRequestIds = copyOf(starredRequestIds);
  }

  public Set<String> getStarredRequestIds() {
    return starredRequestIds;
  }
}
