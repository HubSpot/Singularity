package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.mesos.Resources;

public class SingularityRequest {
  
  private final String command;
  private final Optional<Resources> resources;
  
  @JsonCreator
  public SingularityRequest(@JsonProperty("command") String command, @JsonProperty("resources") Optional<Resources> resources) {
    this.command = command;
    this.resources = resources;
  }

  public Optional<Resources> getResources() {
    return resources;
  }

  public String getCommand() {
    return command;
  }

}
