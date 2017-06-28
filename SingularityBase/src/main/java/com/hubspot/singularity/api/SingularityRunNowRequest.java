package com.hubspot.singularity.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.mesos.Resources;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityRunNowRequest {

  private final Optional<String> message;
  private final Optional<String> runId;
  private final Optional<List<String>> commandLineArgs;
  private final Optional<Boolean> skipHealthchecks;
  private final Optional<Resources> resources;
  private final Optional<Long> runAt;

  public SingularityRunNowRequest(
      Optional<String> message,
      Optional<Boolean> skipHealthchecks,
      Optional<String> runId,
      Optional<List<String>> commandLineArgs,
      Optional<Resources> resources
  ) {
    this(message, skipHealthchecks, runId, commandLineArgs, resources, Optional.<Long>absent());
  }

  @JsonCreator
  public SingularityRunNowRequest(@JsonProperty("message") Optional<String> message,
                                  @JsonProperty("skipHealthchecks") Optional<Boolean> skipHealthchecks,
                                  @JsonProperty("runId") Optional<String> runId,
                                  @JsonProperty("commandLineArgs") Optional<List<String>> commandLineArgs,
                                  @JsonProperty("resources") Optional<Resources> resources,
                                  @JsonProperty("runAt") Optional<Long> runAt) {
    this.message = message;
    this.commandLineArgs = commandLineArgs;
    this.runId = runId;
    this.skipHealthchecks = skipHealthchecks;
    this.resources = resources;
    this.runAt = runAt;
  }

  @ApiModelProperty(required=false, value="A message to show to users about why this action was taken")
  public Optional<String> getMessage() {
    return message;
  }

  @ApiModelProperty(required=false, value="An id to associate with this request which will be associated with the corresponding launched tasks")
  public Optional<String> getRunId() {
    return runId;
  }

  @ApiModelProperty(required=false, value="Command line arguments to be passed to the task")
  public Optional<List<String>> getCommandLineArgs() {
    return commandLineArgs;
  }

  @ApiModelProperty(required=false, value="If set to true, healthchecks will be skipped for this task run")
  public Optional<Boolean> getSkipHealthchecks() {
    return skipHealthchecks;
  }

  @ApiModelProperty(required=false, value="Override the resources from the active deploy for this run")
  public Optional<Resources> getResources() {
    return resources;
  }

  @ApiModelProperty(required=false, value="Schedule this task to run at a specified time")
  public Optional<Long> getRunAt() {
    return runAt;
  }

  @Override
  public String toString() {
    return "SingularityRunNowRequest{" +
        "message=" + message +
        ", runId=" + runId +
        ", commandLineArgs=" + commandLineArgs +
        ", skipHealthchecks=" + skipHealthchecks +
        ", resources=" + resources +
        ", runAt=" + runAt +
        '}';
  }
}
