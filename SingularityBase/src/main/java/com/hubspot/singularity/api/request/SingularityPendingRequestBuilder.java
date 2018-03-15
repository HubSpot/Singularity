package com.hubspot.singularity.api.request;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.hubspot.singularity.api.deploy.mesos.Resources;
import com.hubspot.singularity.api.deploy.mesos.SingularityMesosArtifact;
import com.hubspot.singularity.api.logs.SingularityS3UploaderFile;
import com.hubspot.singularity.api.request.SingularityPendingRequest.PendingType;

public class SingularityPendingRequestBuilder {

  private String requestId;
  private String deployId;
  private long timestamp;
  private PendingType pendingType;
  private Optional<String> user;
  private Optional<List<String>> cmdLineArgsList;
  private Optional<String> runId;
  private Optional<Boolean> skipHealthchecks;
  private Optional<String> message;
  private Optional<String> actionId;
  private Optional<Resources> resources;
  private List<SingularityS3UploaderFile> s3UploaderAdditionalFiles;
  private Optional<String> runAsUserOverride;
  private Map<String, String> envOverrides;
  private List<SingularityMesosArtifact> extraArtifacts;
  private Optional<Long> runAt;

  public SingularityPendingRequestBuilder() {
    this.user = Optional.empty();
    this.cmdLineArgsList = Optional.empty();
    this.runId = Optional.empty();
    this.skipHealthchecks = Optional.empty();
    this.message = Optional.empty();
    this.actionId = Optional.empty();
    this.resources = Optional.empty();
    this.runAsUserOverride = Optional.empty();
    this.envOverrides = Collections.emptyMap();
    this.extraArtifacts = Collections.emptyList();
    this.runAt = Optional.empty();
  }

  public SingularityPendingRequestBuilder setRequestId(String requestId) {
    this.requestId = requestId;
    return this;
  }

  public SingularityPendingRequestBuilder setDeployId(String deployId) {
    this.deployId = deployId;
    return this;
  }

  public SingularityPendingRequestBuilder setTimestamp(long timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  public SingularityPendingRequestBuilder setPendingType(PendingType pendingType) {
    this.pendingType = pendingType;
    return this;
  }

  public SingularityPendingRequestBuilder setUser(String user) {
    this.user = Optional.of(user);
    return this;
  }

  public SingularityPendingRequestBuilder setUser(Optional<String> user) {
    this.user = user;
    return this;
  }

  public SingularityPendingRequestBuilder setCmdLineArgsList(List<String> cmdLineArgsList) {
    this.cmdLineArgsList = Optional.of(cmdLineArgsList);
    return this;
  }

  public SingularityPendingRequestBuilder setCmdLineArgsList(Optional<List<String>> cmdLineArgsList) {
    this.cmdLineArgsList = cmdLineArgsList;
    return this;
  }

  public SingularityPendingRequestBuilder setRunId(String runId) {
    this.runId = Optional.of(runId);
    return this;
  }

  public SingularityPendingRequestBuilder setRunId(Optional<String> runId) {
    this.runId = runId;
    return this;
  }

  public SingularityPendingRequestBuilder setSkipHealthchecks(Boolean skipHealthchecks) {
    this.skipHealthchecks = Optional.of(skipHealthchecks);
    return this;
  }

  public SingularityPendingRequestBuilder setSkipHealthchecks(Optional<Boolean> skipHealthchecks) {
    this.skipHealthchecks = skipHealthchecks;
    return this;
  }

  public SingularityPendingRequestBuilder setMessage(String message) {
    this.message = Optional.of(message);
    return this;
  }

  public SingularityPendingRequestBuilder setMessage(Optional<String> message) {
    this.message = message;
    return this;
  }

  public SingularityPendingRequestBuilder setActionId(String actionId) {
    this.actionId = Optional.of(actionId);
    return this;
  }

  public SingularityPendingRequestBuilder setResources(Resources resources) {
    this.resources = Optional.of(resources);
    return this;
  }

  public SingularityPendingRequestBuilder setResources(Optional<Resources> resources) {
    this.resources = resources;
    return this;
  }

  public SingularityPendingRequestBuilder setS3UploaderAdditionalFiles(List<SingularityS3UploaderFile> s3UploaderAdditionalFiles) {
    this.s3UploaderAdditionalFiles = s3UploaderAdditionalFiles;
    return this;
  }

  public SingularityPendingRequestBuilder setRunAsUserOverride(Optional<String> runAsUserOverride) {
    this.runAsUserOverride = runAsUserOverride;
    return this;
  }

  public SingularityPendingRequestBuilder setEnvOverrides(Map<String, String> envOverrides) {
    this.envOverrides = envOverrides;
    return this;
  }

  public SingularityPendingRequestBuilder setExtraArtifacts(List<SingularityMesosArtifact> extraArtifacts) {
    this.extraArtifacts = extraArtifacts;
    return this;
  }

  public SingularityPendingRequestBuilder setRunAt(Long runAt) {
    this.runAt = Optional.of(runAt);
    return this;
  }

  public SingularityPendingRequestBuilder setRunAt(Optional<Long> runAt) {
    this.runAt = runAt;
    return this;
  }

  public SingularityPendingRequest build() {
    return new SingularityPendingRequest(
        requestId,
        deployId,
        timestamp,
        user,
        pendingType,
        cmdLineArgsList,
        runId,
        skipHealthchecks,
        message,
        actionId,
        resources,
        s3UploaderAdditionalFiles,
        runAsUserOverride,
        envOverrides,
        extraArtifacts,
        runAt
    );
  }

  @Override
  public String toString() {
    return "SingularityPendingRequestBuilder{" +
        "requestId" + requestId +
        ", deployId" + deployId +
        ", timestamp" + timestamp +
        ", pendingType" + pendingType +
        ", user" + user +
        ", cmdLineArgsList" + cmdLineArgsList +
        ", runId" + runId +
        ", skipHealthchecks" + skipHealthchecks +
        ", message" + message +
        ", actionId" + actionId +
        ", resources" + resources +
        ", s3UploaderAdditionalFiles=" + s3UploaderAdditionalFiles +
        ", runAsUserOverride" + runAsUserOverride +
        ", envOverrides" + envOverrides +
        ", extraArtifacts" + extraArtifacts +
        ", runAt" + runAt +
        "}";
  }
}
