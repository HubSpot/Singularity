package com.hubspot.singularity;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.SingularityMesosArtifact;
import com.hubspot.singularity.api.SingularityRunNowRequest;

public class SingularityRunNowRequestBuilder {
  private Optional<String> message;
  private Optional<String> runId;
  private Optional<List<String>> commandLineArgs;
  private Optional<Boolean> skipHealthchecks;
  private Optional<Resources> resources;
  private Optional<String> runAsUserOverride;
  private Map<String, String> envOverrides;
  private List<SingularityMesosArtifact> extraArtifacts;
  private Optional<Long> runAt;

  public SingularityRunNowRequestBuilder()
  {
    this.message = Optional.absent();
    this.runId = Optional.absent();
    this.commandLineArgs = Optional.absent();
    this.skipHealthchecks = Optional.absent();
    this.resources = Optional.absent();
    this.runAsUserOverride = Optional.absent();
    this.envOverrides = Collections.emptyMap();
    this.extraArtifacts = Collections.emptyList();
    this.runAt = Optional.absent();
  }

  public SingularityRunNowRequestBuilder setMessage(String message) {
    this.message = Optional.of(message);
    return this;
  }

  public SingularityRunNowRequestBuilder setRunId(String runId) {
    this.runId = Optional.of(runId);
    return this;
  }

  public SingularityRunNowRequestBuilder setCommandLineArgs(List<String> commandLineArgs) {
    this.commandLineArgs = Optional.of(commandLineArgs);
    return this;
  }

  public SingularityRunNowRequestBuilder setSkipHealthchecks(Boolean skipHealthchecks) {
    this.skipHealthchecks = Optional.of(skipHealthchecks);
    return this;
  }

  public SingularityRunNowRequestBuilder setResources(Resources resources) {
    this.resources = Optional.of(resources);
    return this;
  }

  public SingularityRunNowRequestBuilder setRunAsUserOverride(Optional<String> runAsUserOverride) {
    this.runAsUserOverride = runAsUserOverride;
    return this;
  }

  public SingularityRunNowRequestBuilder setEnvOverrides(Map<String, String> environmentVariables) {
    this.envOverrides = environmentVariables;
    return this;
  }

  public SingularityRunNowRequestBuilder setExtraArtifacts(List<SingularityMesosArtifact> extraArtifacts) {
    this.extraArtifacts = extraArtifacts;
    return this;
  }

  public SingularityRunNowRequestBuilder setRunAt(Long runAt) {
    this.runAt = Optional.of(runAt);
    return this;
  }

  public SingularityRunNowRequest build() {
    return new SingularityRunNowRequest(
        message, skipHealthchecks, runId, commandLineArgs, resources, runAsUserOverride, envOverrides, extraArtifacts, runAt);
  }

  @Override
  public String toString() {
    return "SingularityRunNowRequestBuilder{" +
        "message=" + message +
        ", runId=" + runId +
        ", commandLineArgs=" + commandLineArgs +
        ", skipHealthchecks=" + skipHealthchecks +
        ", resources=" + resources +
        ", runNowUserOverride=" + runAsUserOverride +
        ", envOverrides=" + envOverrides +
        ", extraArtifacts=" + extraArtifacts +
        ", runAt=" + runAt +
        "}";
  }
}
