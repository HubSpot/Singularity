package com.hubspot.singularity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.SingularityMesosArtifact;

public class SingularityPendingRequest {

  public enum PendingType {
    IMMEDIATE, ONEOFF, BOUNCE, NEW_DEPLOY, NEXT_DEPLOY_STEP, UNPAUSED, RETRY, UPDATED_REQUEST, DECOMISSIONED_SLAVE_OR_RACK, TASK_DONE, STARTUP, CANCEL_BOUNCE, TASK_BOUNCE, DEPLOY_CANCELLED, DEPLOY_FAILED;
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
  private final Optional<String> runAsUserOverride;
  private final Map<String, String> envOverrides;
  private final List<SingularityMesosArtifact> extraArtifacts;
  private final Optional<Long> runAt;

  public SingularityPendingRequest(String requestId, String deployId, long timestamp, Optional<String> user, PendingType pendingType, Optional<Boolean> skipHealthchecks, Optional<String> message) {
    this(requestId, deployId, timestamp, user, pendingType, Optional.<List<String>> absent(), Optional.<String> absent(), skipHealthchecks, message, Optional.<String> absent(), Optional.<Resources>absent(), Optional.absent(), null, null, Optional.<Long> absent());
  }

  public SingularityPendingRequest(String requestId, String deployId, long timestamp, Optional<String> user, PendingType pendingType, Optional<List<String>> cmdLineArgsList,
    Optional<String> runId, Optional<Boolean> skipHealthchecks, Optional<String> message, Optional<String> actionId) {
    this(requestId, deployId, timestamp, user, pendingType, cmdLineArgsList, runId, skipHealthchecks, message, actionId, Optional.<Resources>absent(), Optional.absent(), null, null, Optional.<Long>absent());
  }

  @JsonCreator
  public SingularityPendingRequest(@JsonProperty("requestId") String requestId,
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
                                   @JsonProperty("runAsUserOverride") Optional<String> runAsUserOverride,
                                   @JsonProperty("envOverrides") Map<String, String> envOverrides,
                                   @JsonProperty("extraArtifacts") List<SingularityMesosArtifact> extraArtifacts,
                                   @JsonProperty("runAt") Optional<Long> runAt) {
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
    this.runAsUserOverride = runAsUserOverride;

    if (Objects.nonNull(envOverrides)) {
      this.envOverrides = envOverrides;
    } else {
      this.envOverrides = Collections.emptyMap();
    }

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

  public Map<String, String> getEnvOverrides() {
    return envOverrides;
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
    return "SingularityPendingRequest{" +
        "requestId='" + requestId + '\'' +
        ", deployId='" + deployId + '\'' +
        ", timestamp=" + timestamp +
        ", pendingType=" + pendingType +
        ", user=" + user +
        ", cmdLineArgsList=" + cmdLineArgsList +
        ", runId=" + runId +
        ", skipHealthchecks=" + skipHealthchecks +
        ", message=" + message +
        ", actionId=" + actionId +
        ", resources=" + resources +
        ", runAsUserOverride=" + runAsUserOverride +
        ", envOverrides=" + envOverrides +
        ", extraArtifacts=" + extraArtifacts +
        ", runAt=" + runAt +
        '}';
  }
}
