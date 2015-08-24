package com.hubspot.singularity.config;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.ResourceSchedulerType;
import com.hubspot.singularity.SlavePlacement;

public class ResourceSchedulerConfiguration {
  @JsonProperty
  @NotNull
  private ResourceSchedulerType type = ResourceSchedulerType.SINGULARITY;

  @JsonProperty
  private Optional<SlavePlacement> defaultSlavePlacement;

  @JsonProperty
  private Optional<Integer> maxTasksPerOffer;

  public ResourceSchedulerType getType() {
    return type;
  }

  public void setType(ResourceSchedulerType type) {
    this.type = type;
  }

  public Optional<SlavePlacement> getDefaultSlavePlacement() {
    return defaultSlavePlacement;
  }

  public void setDefaultSlavePlacement(Optional<SlavePlacement> defaultSlavePlacement) {
    this.defaultSlavePlacement = defaultSlavePlacement;
  }

  public Optional<Integer> getMaxTasksPerOffer() {
    return maxTasksPerOffer;
  }

  public void setMaxTasksPerOffer(Optional<Integer> maxTasksPerOffer) {
    this.maxTasksPerOffer = maxTasksPerOffer;
  }
}
