package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityLDAPPoolStats {
  private final int numActiveConnections;
  private final int numIdleConnections;

  @JsonCreator
  public SingularityLDAPPoolStats(@JsonProperty("numActiveConnections") int numActiveConnections,
                                  @JsonProperty("numIdleConnections") int numIdleConnections) {
    this.numActiveConnections = numActiveConnections;
    this.numIdleConnections = numIdleConnections;
  }

  public int getNumActiveConnections() {
    return numActiveConnections;
  }

  public int getNumIdleConnections() {
    return numIdleConnections;
  }
}
