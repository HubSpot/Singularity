package com.hubspot.singularity;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.mesos.Resources;

public class SingularityRequest {
  
  @NotNull
  private final String command;
  
  @NotNull
  private final String name;
  
  private final String executor;
  private final Resources resources;
  
  @JsonCreator
  public SingularityRequest(@JsonProperty("command") String command, @JsonProperty("name") String name, @JsonProperty("executor") String executor, @JsonProperty("resources") Resources resources) {
    this.command = command;
    this.name = name;
    this.resources = resources;
    this.executor = executor;
  }

  public String getName() {
    return name;
  }

  public String getExecutor() {
    return executor;
  }

  public Resources getResources() {
    return resources;
  }

  public String getCommand() {
    return command;
  }

}
