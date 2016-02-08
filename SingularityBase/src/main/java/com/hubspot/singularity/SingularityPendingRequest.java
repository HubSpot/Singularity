package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityPendingRequest {

  public enum PendingType {
    IMMEDIATE, ONEOFF, BOUNCE, NEW_DEPLOY, NEXT_DEPLOY_STEP, UNPAUSED, RETRY, UPDATED_REQUEST, DECOMISSIONED_SLAVE_OR_RACK, TASK_DONE, STARTUP, CANCEL_BOUNCE, TASK_BOUNCE, DEPLOY_CANCELLED;
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

  public SingularityPendingRequest(String requestId, String deployId, long timestamp, Optional<String> user, PendingType pendingType, Optional<Boolean> skipHealthchecks, Optional<String> message) {
    this(requestId, deployId, timestamp, user, pendingType, Optional.<List<String>> absent(), Optional.<String> absent(), skipHealthchecks, message, Optional.<String> absent());
  }

  @JsonCreator
  public SingularityPendingRequest(@JsonProperty("requestId") String requestId, @JsonProperty("deployId") String deployId, @JsonProperty("timestamp") long timestamp,
      @JsonProperty("user") Optional<String> user, @JsonProperty("pendingType") PendingType pendingType, @JsonProperty("cmdLineArgsList") Optional<List<String>> cmdLineArgsList,
      @JsonProperty("runId") Optional<String> runId, @JsonProperty("skipHealthchecks") Optional<Boolean> skipHealthchecks, @JsonProperty("message") Optional<String> message,
      @JsonProperty("actionId") Optional<String> actionId) {
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

  @Override
  public String toString() {
    return "SingularityPendingRequest [requestId=" + requestId + ", deployId=" + deployId + ", timestamp=" + timestamp + ", pendingType=" + pendingType + ", user=" + user + ", cmdLineArgsList="
        + cmdLineArgsList + ", runId=" + runId + ", skipHealthchecks=" + skipHealthchecks + ", message=" + message + ", actionId=" + actionId + "]";
  }

}
