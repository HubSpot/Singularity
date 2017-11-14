package com.hubspot.singularity;

import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;
import com.hubspot.mesos.Resources;
import com.hubspot.singularity.api.SingularityRunNowRequest;

public class SingularityRunNowRequestBuilder {
  private Optional<String> message;
  private Optional<String> runId;
  private Optional<List<String>> commandLineArgs;
  private Optional<Boolean> skipHealthchecks;
  private Optional<Resources> resources;
  private Optional<Map<String, String>> environmentVariables;
  private Optional<Long> runAt;

  public SingularityRunNowRequestBuilder()
  {
    this.message = Optional.absent();
    this.runId = Optional.absent();
    this.commandLineArgs = Optional.absent();
    this.skipHealthchecks = Optional.absent();
    this.resources = Optional.absent();
    this.environmentVariables = Optional.absent();
    this.runAt = Optional.absent();
  }

  public SingularityRunNowRequestBuilder message(String message) {
    this.message = Optional.of(message);
    return this;
  }

  public SingularityRunNowRequestBuilder runId(String runId) {
    this.runId = Optional.of(runId);
    return this;
  }

  public SingularityRunNowRequestBuilder commandLineArgs(List<String> commandLineArgs) {
    this.commandLineArgs = Optional.of(commandLineArgs);
    return this;
  }

  public SingularityRunNowRequestBuilder skipHealthchecks(Boolean skipHealthchecks) {
    this.skipHealthchecks = Optional.of(skipHealthchecks);
    return this;
  }

  public SingularityRunNowRequestBuilder resources(Resources resources) {
    this.resources = Optional.of(resources);
    return this;
  }

  public SingularityRunNowRequestBuilder environmentVariables(Map<String, String> environmentVariables) {
    this.environmentVariables = Optional.of(environmentVariables);
    return this;
  }

  public SingularityRunNowRequestBuilder runAt(Long runAt) {
    this.runAt = Optional.of(runAt);
    return this;
  }

  public SingularityRunNowRequest build() {
    return new SingularityRunNowRequest(
        message, skipHealthchecks, runId, commandLineArgs, resources, environmentVariables, runAt);
  }

}
