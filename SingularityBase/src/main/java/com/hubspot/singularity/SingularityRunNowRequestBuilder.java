package com.hubspot.singularity;

import com.hubspot.mesos.Resources;
import com.hubspot.mesos.SingularityMesosArtifact;
import com.hubspot.singularity.api.SingularityRunNowRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SingularityRunNowRequestBuilder {
  private Optional<String> message;
  private Optional<String> runId;
  private Optional<List<String>> commandLineArgs;
  private Optional<Boolean> skipHealthchecks;
  private Optional<Resources> resources;
  private List<SingularityS3UploaderFile> s3UploaderAdditionalFiles;
  private Optional<String> runAsUserOverride;
  private Map<String, String> envOverrides;
  private Map<String, String> requiredSlaveAttributeOverrides;
  private Map<String, String> allowedSlaveAttributeOverrides;
  private List<SingularityMesosArtifact> extraArtifacts;
  private Optional<Long> runAt;

  public SingularityRunNowRequestBuilder() {
    this.message = Optional.empty();
    this.runId = Optional.empty();
    this.commandLineArgs = Optional.empty();
    this.skipHealthchecks = Optional.empty();
    this.resources = Optional.empty();
    this.runAsUserOverride = Optional.empty();
    this.envOverrides = Collections.emptyMap();
    this.requiredSlaveAttributeOverrides = Collections.emptyMap();
    this.allowedSlaveAttributeOverrides = Collections.emptyMap();
    this.extraArtifacts = Collections.emptyList();
    this.runAt = Optional.empty();
  }

  public SingularityRunNowRequestBuilder setMessage(String message) {
    this.message = Optional.of(message);
    return this;
  }

  public SingularityRunNowRequestBuilder setRunId(String runId) {
    this.runId = Optional.of(runId);
    return this;
  }

  public SingularityRunNowRequestBuilder setCommandLineArgs(
    List<String> commandLineArgs
  ) {
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

  public SingularityRunNowRequestBuilder setS3UploaderAdditionalFiles(
    List<SingularityS3UploaderFile> s3UploaderAdditionalFiles
  ) {
    this.s3UploaderAdditionalFiles = s3UploaderAdditionalFiles;
    return this;
  }

  public SingularityRunNowRequestBuilder setRunAsUserOverride(
    Optional<String> runAsUserOverride
  ) {
    this.runAsUserOverride = runAsUserOverride;
    return this;
  }

  public SingularityRunNowRequestBuilder setEnvOverrides(
    Map<String, String> environmentVariables
  ) {
    this.envOverrides = environmentVariables;
    return this;
  }

  public SingularityRunNowRequestBuilder setRequiredSlaveAttributeOverrides(
    Map<String, String> requiredSlaveAttributeOverrides
  ) {
    this.requiredSlaveAttributeOverrides = requiredSlaveAttributeOverrides;
    return this;
  }

  public SingularityRunNowRequestBuilder setAllowedSlaveAttributeOverrides(
    Map<String, String> allowedSlaveAttributeOverrides
  ) {
    this.allowedSlaveAttributeOverrides = allowedSlaveAttributeOverrides;
    return this;
  }

  public SingularityRunNowRequestBuilder setExtraArtifacts(
    List<SingularityMesosArtifact> extraArtifacts
  ) {
    this.extraArtifacts = extraArtifacts;
    return this;
  }

  public SingularityRunNowRequestBuilder setRunAt(Long runAt) {
    this.runAt = Optional.of(runAt);
    return this;
  }

  public SingularityRunNowRequest build() {
    return new SingularityRunNowRequest(
      message,
      skipHealthchecks,
      runId,
      commandLineArgs,
      resources,
      s3UploaderAdditionalFiles,
      runAsUserOverride,
      envOverrides,
      requiredSlaveAttributeOverrides,
      allowedSlaveAttributeOverrides,
      extraArtifacts,
      runAt
    );
  }

  @Override
  public String toString() {
    return (
      "SingularityRunNowRequestBuilder{" +
      "message=" +
      message +
      ", runId=" +
      runId +
      ", commandLineArgs=" +
      commandLineArgs +
      ", skipHealthchecks=" +
      skipHealthchecks +
      ", resources=" +
      resources +
      ", s3UploaderAdditionalFiles=" +
      s3UploaderAdditionalFiles +
      ", runNowUserOverride=" +
      runAsUserOverride +
      ", envOverrides=" +
      envOverrides +
      ", requiredSlaveAttributeOverrides=" +
      requiredSlaveAttributeOverrides +
      ", allowedSlaveAttributeOverrides=" +
      allowedSlaveAttributeOverrides +
      ", extraArtifacts=" +
      extraArtifacts +
      ", runAt=" +
      runAt +
      "}"
    );
  }
}
