package com.hubspot.singularity.api;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityShellCommand;

public class SingularityBounceRequestBuilder {

  private Optional<Boolean> incremental;
  private Optional<Boolean> skipHealthchecks;
  private Optional<SingularityShellCommand> runShellCommandBeforeKill;

  private Optional<Long> durationMillis;
  private Optional<String> actionId;
  private Optional<String> message;

  public SingularityBounceRequestBuilder() {
    this.incremental = Optional.absent();
    this.skipHealthchecks = Optional.absent();
    this.runShellCommandBeforeKill = Optional.absent();

    this.durationMillis = Optional.absent();
    this.actionId = Optional.absent();
    this.message = Optional.absent();
  }

  public SingularityBounceRequest build() {
    return new SingularityBounceRequest(incremental, skipHealthchecks, durationMillis, actionId, message, runShellCommandBeforeKill);
  }

  public Optional<Long> getDurationMillis() {
    return durationMillis;
  }

  public SingularityBounceRequestBuilder setDurationMillis(Optional<Long> durationMillis) {
    this.durationMillis = durationMillis;
    return this;
  }

  public Optional<String> getActionId() {
    return actionId;
  }

  public SingularityBounceRequestBuilder setActionId(Optional<String> actionId) {
    this.actionId = actionId;
    return this;
  }

  public Optional<String> getMessage() {
    return message;
  }

  public SingularityBounceRequestBuilder setMessage(Optional<String> message) {
    this.message = message;
    return this;
  }

  public Optional<Boolean> getIncremental() {
    return incremental;
  }

  public SingularityBounceRequestBuilder setIncremental(Optional<Boolean> incremental) {
    this.incremental = incremental;
    return this;
  }

  public Optional<Boolean> getSkipHealthchecks() {
    return skipHealthchecks;
  }

  public SingularityBounceRequestBuilder setSkipHealthchecks(Optional<Boolean> skipHealthchecks) {
    this.skipHealthchecks = skipHealthchecks;
    return this;
  }

  public Optional<SingularityShellCommand> getRunShellCommandBeforeKill() {
    return runShellCommandBeforeKill;
  }

  public SingularityBounceRequestBuilder setRunShellCommandBeforeKill(Optional<SingularityShellCommand> runShellCommandBeforeKill) {
    this.runShellCommandBeforeKill = runShellCommandBeforeKill;
    return this;
  }

  @Override
  public String toString() {
    return "SingularityBounceRequestBuilder{" +
        "incremental=" + incremental +
        ", skipHealthchecks=" + skipHealthchecks +
        ", runShellCommandBeforeKill=" + runShellCommandBeforeKill +
        ", durationMillis=" + durationMillis +
        ", actionId=" + actionId +
        ", message=" + message +
        '}';
  }
}
