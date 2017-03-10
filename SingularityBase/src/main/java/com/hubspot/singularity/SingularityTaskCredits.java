package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityTaskCredits {
  private final boolean enabled;
  private final int remaining;

  @JsonCreator
  public SingularityTaskCredits(@JsonProperty("enabled") boolean enabled, @JsonProperty("remaining") int remaining) {
    this.enabled = enabled;
    this.remaining = remaining;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public int getRemaining() {
    return remaining;
  }

  @Override
  public String toString() {
    return "SingularityTaskCredits{" +
        "enabled=" + enabled +
        ", remaining=" + remaining +
        '}';
  }
}
