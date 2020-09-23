package com.hubspot.singularity;

import com.hubspot.mesos.Resources;
import com.hubspot.mesos.SingularityMesosArtifact;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SingularityPendingTaskBuilder {
  private SingularityPendingTaskId pendingTaskId;
  private Optional<List<String>> cmdLineArgsList;
  private Optional<String> user;
  private Optional<String> runId;
  private Optional<Boolean> skipHealthchecks;
  private Optional<String> message;
  private Optional<Resources> resources;
  private List<SingularityS3UploaderFile> s3UploaderAdditionalFiles;
  private Optional<String> runAsUserOverride;
  private Map<String, String> envOverrides;
  private Map<String, String> requiredAgentAttributeOverrides;
  private Map<String, String> allowedAgentAttributeOverrides;
  private List<SingularityMesosArtifact> extraArtifacts;
  private Optional<String> actionId;

  public SingularityPendingTaskBuilder() {
    this.pendingTaskId = null;
    this.cmdLineArgsList = Optional.empty();
    this.user = Optional.empty();
    this.runId = Optional.empty();
    this.skipHealthchecks = Optional.empty();
    this.message = Optional.empty();
    this.resources = Optional.empty();
    this.s3UploaderAdditionalFiles = Collections.emptyList();
    this.runAsUserOverride = Optional.empty();
    this.envOverrides = Collections.emptyMap();
    this.extraArtifacts = Collections.emptyList();
    this.actionId = Optional.empty();
  }

  public SingularityPendingTaskBuilder setPendingTaskId(
    SingularityPendingTaskId pendingTaskId
  ) {
    this.pendingTaskId = pendingTaskId;
    return this;
  }

  public SingularityPendingTaskBuilder setCmdLineArgsList(List<String> cmdLineArgsList) {
    this.cmdLineArgsList = Optional.of(cmdLineArgsList);
    return this;
  }

  public SingularityPendingTaskBuilder setCmdLineArgsList(
    Optional<List<String>> cmdLineArgsList
  ) {
    this.cmdLineArgsList = cmdLineArgsList;
    return this;
  }

  public SingularityPendingTaskBuilder setUser(String user) {
    this.user = Optional.of(user);
    return this;
  }

  public SingularityPendingTaskBuilder setRunId(String runId) {
    this.runId = Optional.of(runId);
    return this;
  }

  public SingularityPendingTaskBuilder setRunId(Optional<String> runId) {
    this.runId = runId;
    return this;
  }

  public SingularityPendingTaskBuilder setSkipHealthchecks(Boolean skipHealthchecks) {
    this.skipHealthchecks = Optional.of(skipHealthchecks);
    return this;
  }

  public SingularityPendingTaskBuilder setMessage(String message) {
    this.message = Optional.of(message);
    return this;
  }

  public SingularityPendingTaskBuilder setResources(Resources resources) {
    this.resources = Optional.of(resources);
    return this;
  }

  public SingularityPendingTaskBuilder setS3UploaderAdditionalFiles(
    List<SingularityS3UploaderFile> s3UploaderAdditionalFiles
  ) {
    this.s3UploaderAdditionalFiles = s3UploaderAdditionalFiles;
    return this;
  }

  public SingularityPendingTaskBuilder setRunAsUserOverride(
    Optional<String> runAsUserOverride
  ) {
    this.runAsUserOverride = runAsUserOverride;
    return this;
  }

  public SingularityPendingTaskBuilder setEnvOverrides(Map<String, String> envOverrides) {
    this.envOverrides = envOverrides;
    return this;
  }

  @Deprecated
  public SingularityPendingTaskBuilder setRequiredSlaveAttributeOverrides(
    Map<String, String> requiredAgentAttributeOverrides
  ) {
    this.requiredAgentAttributeOverrides = requiredAgentAttributeOverrides;
    return this;
  }

  @Deprecated
  public SingularityPendingTaskBuilder setAllowedSlaveAttributeOverrides(
    Map<String, String> allowedAgentAttributeOverrides
  ) {
    this.allowedAgentAttributeOverrides = allowedAgentAttributeOverrides;
    return this;
  }

  public SingularityPendingTaskBuilder setRequiredAgentAttributeOverrides(
    Map<String, String> requiredAgentAttributeOverrides
  ) {
    this.requiredAgentAttributeOverrides = requiredAgentAttributeOverrides;
    return this;
  }

  public SingularityPendingTaskBuilder setAllowedAgentAttributeOverrides(
    Map<String, String> allowedAgentAttributeOverrides
  ) {
    this.allowedAgentAttributeOverrides = allowedAgentAttributeOverrides;
    return this;
  }

  public SingularityPendingTaskBuilder setExtraArtifacts(
    List<SingularityMesosArtifact> extraArtifacts
  ) {
    this.extraArtifacts = extraArtifacts;
    return this;
  }

  public SingularityPendingTaskBuilder setActionId(String actionId) {
    this.actionId = Optional.of(actionId);
    return this;
  }

  public SingularityPendingTask build() {
    return new SingularityPendingTask(
      pendingTaskId,
      cmdLineArgsList,
      user,
      runId,
      skipHealthchecks,
      message,
      resources,
      s3UploaderAdditionalFiles,
      runAsUserOverride,
      envOverrides,
      requiredAgentAttributeOverrides,
      allowedAgentAttributeOverrides,
      extraArtifacts,
      actionId
    );
  }

  @Override
  public String toString() {
    return (
      "SingularityPendingTask{" +
      "pendingTaskId=" +
      pendingTaskId +
      ", cmdLineArgsList=" +
      cmdLineArgsList +
      ", user=" +
      user +
      ", runId=" +
      runId +
      ", skipHealthchecks=" +
      skipHealthchecks +
      ", message=" +
      message +
      ", resources=" +
      resources +
      ", s3UploaderAdditionalFiles=" +
      s3UploaderAdditionalFiles +
      ", runAsUserOverride=" +
      runAsUserOverride +
      ", envOverrides=" +
      envOverrides +
      ", requiredAgentAttributeOverrides=" +
      requiredAgentAttributeOverrides +
      ", allowedAgentAttributeOverrides=" +
      allowedAgentAttributeOverrides +
      ", extraArtifacts=" +
      extraArtifacts +
      ", actionId=" +
      actionId +
      '}'
    );
  }
}
