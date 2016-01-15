package com.hubspot.singularity.executor.shells;

import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
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
  private boolean skipCommandPrefix = false;

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

  public boolean isSkipCommandPrefix() {
    return skipCommandPrefix;
  }

  public void setSkipCommandPrefix(boolean skipCommandPrefix) {
    this.skipCommandPrefix = skipCommandPrefix;
  }
}
