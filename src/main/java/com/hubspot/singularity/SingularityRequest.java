package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.mesos.Protos;

import java.util.List;

public class SingularityRequest {
  private final String command;

  @JsonCreator
  public SingularityRequest(@JsonProperty("command") String command) {
    this.command = command;
  }

  public String getCommand() {
    return command;
  }
}
