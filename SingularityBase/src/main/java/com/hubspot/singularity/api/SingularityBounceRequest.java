package com.hubspot.singularity.api;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityShellCommand;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityBounceRequest extends SingularityExpiringRequestParent {

  private final Optional<Boolean> incremental;
  private final Optional<Boolean> skipHealthchecks;
  private final Optional<SingularityShellCommand> runShellCommandBeforeKill;

  @JsonCreator
  public SingularityBounceRequest(@JsonProperty("incremental") Optional<Boolean> incremental, @JsonProperty("skipHealthchecks") Optional<Boolean> skipHealthchecks,
      @JsonProperty("durationMillis") Optional<Long> durationMillis, @JsonProperty("actionId") Optional<String> actionId, @JsonProperty("message") Optional<String> message,
      @JsonProperty("runShellCommandBeforeKill") Optional<SingularityShellCommand> runShellCommandBeforeKill) {
    super(durationMillis, actionId, message);
    this.incremental = incremental;
    this.skipHealthchecks = skipHealthchecks;
    this.runShellCommandBeforeKill = runShellCommandBeforeKill;
  }

  public static SingularityBounceRequest defaultRequest() {
    return new SingularityBounceRequest(Optional.<Boolean>absent(), Optional.<Boolean>absent(), Optional.<Long>absent(), Optional.of(UUID.randomUUID().toString()), Optional.<String>absent(), Optional.<SingularityShellCommand>absent());
  }

  public SingularityBounceRequestBuilder toBuilder() {
    return new SingularityBounceRequestBuilder()
        .setIncremental(incremental)
        .setSkipHealthchecks(skipHealthchecks)
        .setDurationMillis(getDurationMillis())
        .setActionId(getActionId())
        .setMessage(getMessage())
        .setRunShellCommandBeforeKill(getRunShellCommandBeforeKill());
  }

  @ApiModelProperty(required=false, value="If present and set to true, old tasks will be killed as soon as replacement tasks are available, instead of waiting for all replacement tasks to be healthy")
  public Optional<Boolean> getIncremental() {
    return incremental;
  }

  @ApiModelProperty(required=false, value="Instruct replacement tasks for this bounce only to skip healthchecks")
  public Optional<Boolean> getSkipHealthchecks() {
    return skipHealthchecks;
  }

  @ApiModelProperty(required=false, value="Attempt to run this shell command on each task before it is shut down")
  public Optional<SingularityShellCommand> getRunShellCommandBeforeKill() {
    return runShellCommandBeforeKill;
  }

  @Override
  public String toString() {
    return "SingularityBounceRequest{" +
        "incremental=" + incremental +
        ", skipHealthchecks=" + skipHealthchecks +
        ", runShellCommandBeforeKill=" + runShellCommandBeforeKill +
        "} " + super.toString();
  }
}
