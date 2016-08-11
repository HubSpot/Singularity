package com.hubspot.singularity;

import static com.google.common.collect.ImmutableSet.copyOf;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityUserSettings {
  private final Set<String> starredRequestIds;

  @JsonCreator
  public SingularityUserSettings(@JsonProperty Set<String> starredRequestIds) {
    this.starredRequestIds = copyOf(starredRequestIds);
  }

  public Set<String> getStarredRequestIds() {
    return starredRequestIds;
  }
}
