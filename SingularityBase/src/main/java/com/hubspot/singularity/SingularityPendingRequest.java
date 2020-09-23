package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.SingularityMesosArtifact;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Schema(description = "Describes a task that is waiting to be launched")
public class SingularityPendingRequest {

  @Schema
  public enum PendingType {
    IMMEDIATE,
    ONEOFF,
    BOUNCE,
    NEW_DEPLOY,
    NEXT_DEPLOY_STEP,
    UNPAUSED,
    RETRY,
    UPDATED_REQUEST,
    DECOMISSIONED_SLAVE_OR_RACK,
    TASK_DONE,
    STARTUP,
    CANCEL_BOUNCE,
    TASK_BOUNCE,
    DEPLOY_CANCELLED,
    DEPLOY_FAILED,
    DEPLOY_FINISHED,
    TASK_RECOVERED
  }

  private final String requestId;
  private final String deployId;
  private final long timestamp;
  private final PendingType pendingType;
  private final Optional<String> user;
  private final Optional<List<String>> cmdLineArgsList;
  private final Optional<String> runId;
  private final Optional<Boolean> skipHealthchecks;
  private final Optional<String> message;
  private final Optional<String> actionId;
  private final Optional<Resources> resources;
  private final List<SingularityS3UploaderFile> s3UploaderAdditionalFiles;
  private final Optional<String> runAsUserOverride;
  private final Map<String, String> envOverrides;
  private final Map<String, String> requiredAgentAttributeOverrides;
  private final Map<String, String> allowedAgentAttributeOverrides;
  private final List<SingularityMesosArtifact> extraArtifacts;
  private final Optional<Long> runAt;

  @SuppressFBWarnings("NP_NULL_PARAM_DEREF_NONVIRTUAL")
  public SingularityPendingRequest(
    String requestId,
    String deployId,
    long timestamp,
    Optional<String> user,
    PendingType pendingType,
    Optional<Boolean> skipHealthchecks,
    Optional<String> message
  ) {
    this(
      requestId,
      deployId,
      timestamp,
      user,
      pendingType,
      Optional.<List<String>>empty(),
      Optional.<String>empty(),
      skipHealthchecks,
      message,
      Optional.<String>empty(),
      Optional.<Resources>empty(),
      Collections.emptyList(),
      Optional.empty(),
      null,
      null,
      null,
      null,
      Optional.<Long>empty()
    );
  }

  @SuppressFBWarnings("NP_NULL_PARAM_DEREF_NONVIRTUAL")
  public SingularityPendingRequest(
    String requestId,
    String deployId,
    long timestamp,
    Optional<String> user,
    PendingType pendingType,
    Optional<List<String>> cmdLineArgsList,
    Optional<String> runId,
    Optional<Boolean> skipHealthchecks,
    Optional<String> message,
    Optional<String> actionId
  ) {
    this(
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
      Optional.<Resources>empty(),
      Collections.emptyList(),
      Optional.empty(),
      null,
      null,
      null,
      null,
      Optional.<Long>empty(),
      null,
      null
    );
  }

  @SuppressFBWarnings("NP_NULL_PARAM_DEREF_NONVIRTUAL")
  public SingularityPendingRequest(
    String requestId,
    String deployId,
    long timestamp,
    Optional<String> user,
    PendingType pendingType,
    Optional<List<String>> cmdLineArgsList,
    Optional<String> runId,
    Optional<Boolean> skipHealthchecks,
    Optional<String> message,
    Optional<String> actionId,
    Optional<Resources> resources,
    List<SingularityS3UploaderFile> s3UploaderAdditionalFiles,
    Optional<String> runAsUserOverride,
    Map<String, String> envOverrides,
    Map<String, String> requiredAgentAttributeOverrides,
    Map<String, String> allowedSlaveAttributeOverrides,
    List<SingularityMesosArtifact> extraArtifacts,
    Optional<Long> runAt
  ) {
    this(
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
      null,
      null,
      extraArtifacts,
      runAt,
      requiredAgentAttributeOverrides,
      allowedSlaveAttributeOverrides
    );
  }

  @JsonCreator
  public SingularityPendingRequest(
    @JsonProperty("requestId") String requestId,
    @JsonProperty("deployId") String deployId,
    @JsonProperty("timestamp") long timestamp,
    @JsonProperty("user") Optional<String> user,
    @JsonProperty("pendingType") PendingType pendingType,
    @JsonProperty("cmdLineArgsList") Optional<List<String>> cmdLineArgsList,
    @JsonProperty("runId") Optional<String> runId,
    @JsonProperty("skipHealthchecks") Optional<Boolean> skipHealthchecks,
    @JsonProperty("message") Optional<String> message,
    @JsonProperty("actionId") Optional<String> actionId,
    @JsonProperty("resources") Optional<Resources> resources,
    @JsonProperty(
      "s3UploaderAdditionalFiles"
    ) List<SingularityS3UploaderFile> s3UploaderAdditionalFiles,
    @JsonProperty("runAsUserOverride") Optional<String> runAsUserOverride,
    @JsonProperty("envOverrides") Map<String, String> envOverrides,
    @JsonProperty(
      "requiredSlaveAttributeOverrides"
    ) Map<String, String> requiredSlaveAttributeOverrides,
    @JsonProperty(
      "allowedSlaveAttributeOverrides"
    ) Map<String, String> allowedSlaveAttributeOverrides,
    @JsonProperty("extraArtifacts") List<SingularityMesosArtifact> extraArtifacts,
    @JsonProperty("runAt") Optional<Long> runAt,
    @JsonProperty(
      "requiredAgentAttributeOverrides"
    ) Map<String, String> requiredAgentAttributeOverrides,
    @JsonProperty(
      "allowedAgentAttributeOverrides"
    ) Map<String, String> allowedAgentAttributeOverrides
  ) {
    this.requestId = requestId;
    this.deployId = deployId;
    this.timestamp = timestamp;
    this.user = user;
    this.cmdLineArgsList = cmdLineArgsList;
    this.pendingType = pendingType;
    this.runId = runId;
    this.skipHealthchecks = skipHealthchecks;
    this.message = message;
    this.actionId = actionId;
    this.resources = resources;

    if (Objects.nonNull(s3UploaderAdditionalFiles)) {
      this.s3UploaderAdditionalFiles = s3UploaderAdditionalFiles;
    } else {
      this.s3UploaderAdditionalFiles = Collections.emptyList();
    }

    this.runAsUserOverride = runAsUserOverride;

    if (Objects.nonNull(envOverrides)) {
      this.envOverrides = envOverrides;
    } else {
      this.envOverrides = Collections.emptyMap();
    }

    this.requiredAgentAttributeOverrides =
      MoreObjects.firstNonNull(
        requiredAgentAttributeOverrides,
        MoreObjects.firstNonNull(requiredSlaveAttributeOverrides, Collections.emptyMap())
      );
    this.allowedAgentAttributeOverrides =
      MoreObjects.firstNonNull(
        allowedAgentAttributeOverrides,
        MoreObjects.firstNonNull(allowedSlaveAttributeOverrides, Collections.emptyMap())
      );

    if (Objects.nonNull(extraArtifacts)) {
      this.extraArtifacts = extraArtifacts;
    } else {
      this.extraArtifacts = Collections.emptyList();
    }

    this.runAt = runAt;
  }

  public Optional<String> getActionId() {
    return actionId;
  }

  public Optional<String> getRunId() {
    return runId;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String getDeployId() {
    return deployId;
  }

  public Optional<String> getUser() {
    return user;
  }

  public String getRequestId() {
    return requestId;
  }

  public PendingType getPendingType() {
    return pendingType;
  }

  public Optional<List<String>> getCmdLineArgsList() {
    return cmdLineArgsList;
  }

  public Optional<Boolean> getSkipHealthchecks() {
    return skipHealthchecks;
  }

  public Optional<String> getMessage() {
    return message;
  }

  public Optional<Resources> getResources() {
    return resources;
  }

  public List<SingularityS3UploaderFile> getS3UploaderAdditionalFiles() {
    return s3UploaderAdditionalFiles;
  }

  public Map<String, String> getEnvOverrides() {
    return envOverrides;
  }

  public Map<String, String> getRequiredAgentAttributeOverrides() {
    return requiredAgentAttributeOverrides;
  }

  public Map<String, String> getAllowedAgentAttributeOverrides() {
    return allowedAgentAttributeOverrides;
  }

  @Deprecated
  public Map<String, String> getRequiredSlaveAttributeOverrides() {
    return requiredAgentAttributeOverrides;
  }

  @Deprecated
  public Map<String, String> getAllowedSlaveAttributeOverrides() {
    return allowedAgentAttributeOverrides;
  }

  public Optional<String> getRunAsUserOverride() {
    return runAsUserOverride;
  }

  public List<SingularityMesosArtifact> getExtraArtifacts() {
    return extraArtifacts;
  }

  public Optional<Long> getRunAt() {
    return runAt;
  }

  @Override
  public String toString() {
    return (
      "SingularityPendingRequest{" +
      "requestId='" +
      requestId +
      '\'' +
      ", deployId='" +
      deployId +
      '\'' +
      ", timestamp=" +
      timestamp +
      ", pendingType=" +
      pendingType +
      ", user=" +
      user +
      ", cmdLineArgsList=" +
      cmdLineArgsList +
      ", runId=" +
      runId +
      ", skipHealthchecks=" +
      skipHealthchecks +
      ", message=" +
      message +
      ", actionId=" +
      actionId +
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
      ", runAt=" +
      runAt +
      '}'
    );
  }
}
