package com.hubspot.singularity.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityRunNowRequest {

  private final Optional<String> message;
  private final Optional<String> runId;
  private final Optional<List<String>> commandLineArgs;
  private final Optional<Boolean> skipHealthchecks;

  @JsonCreator
  public SingularityRunNowRequest(@JsonProperty("message") Optional<String> message, @JsonProperty("skipHealthchecks") Optional<Boolean> skipHealthchecks,
      @JsonProperty("runId") Optional<String> runId, @JsonProperty("commandLineArgs") Optional<List<String>> commandLineArgs) {
    this.message = message;
    this.commandLineArgs = commandLineArgs;
    this.runId = runId;
    this.skipHealthchecks = skipHealthchecks;
  }

  public Optional<String> getMessage() {
    return message;
  }

  public Optional<String> getRunId() {
    return runId;
  }

  public Optional<List<String>> getCommandLineArgs() {
    return commandLineArgs;
  }

  public Optional<Boolean> getSkipHealthchecks() {
    return skipHealthchecks;
  }

  @Override
  public String toString() {
    return "SingularityRunNowRequest [message=" + message + ", runId=" + runId + ", commandLineArgs=" + commandLineArgs + ", skipHealthchecks=" + skipHealthchecks + "]";
  }

}
