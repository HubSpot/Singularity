package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityRequestInstances {

  private final String id;
  private final Optional<Integer> instances;

  @JsonCreator
  public SingularityRequestInstances(@JsonProperty("id") String id, @JsonProperty("instances") Optional<Integer> instances) {
    this.id = id;
    this.instances = instances;
  }

  public String getId() {
    return id;
  }

  public Optional<Integer> getInstances() {
    return instances;
  }

  @Override
  public String toString() {
    return "SingularityRequestInstances [id=" + id + ", instances=" + instances + "]";
  }
}
