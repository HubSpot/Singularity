package com.hubspot.singularity.executor.shells;

import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityExecutorShellCommandDescriptor {

  @JsonProperty
  @NotNull
  private String name;

  @JsonProperty
  @NotNull
  // Options are : {PID}
  private List<String> command;

  @JsonProperty
  @NotNull
  private List<SingularityExecutorShellCommandOptionDescriptor> options = Collections.emptyList();

  @JsonProperty
  @NotNull
  private boolean docker = false;

  public List<SingularityExecutorShellCommandOptionDescriptor> getOptions() {
    return options;
  }

  public void setOptions(List<SingularityExecutorShellCommandOptionDescriptor> options) {
    this.options = options;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setCommand(List<String> command) {
    this.command = command;
  }

  public String getName() {
    return name;
  }

  public List<String> getCommand() {
    return command;
  }

  public boolean isDocker() {
    return docker;
  }

  public void setDocker(boolean docker) {
    this.docker = docker;
  }
}
