package com.hubspot.singularity;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityPendingRequest {

  public enum PendingType {
    IMMEDIATE, ONEOFF, BOUNCE, NEW_DEPLOY, NEXT_DEPLOY_STEP, UNPAUSED, RETRY, UPDATED_REQUEST, DECOMISSIONED_SLAVE_OR_RACK, TASK_DONE, STARTUP;
  }

  private final String requestId;
  private final String deployId;
  private final long timestamp;
  private final PendingType pendingType;
  private final Optional<String> user;
  private final List<String> cmdLineArgsList;
  private final Optional<String> runId;

  public SingularityPendingRequest(String requestId, String deployId, long timestamp, PendingType pendingType) {
    this(requestId, deployId, timestamp, Optional.<String> absent(), pendingType, Collections.<String> emptyList(), Optional.<String> absent());
  }

  public SingularityPendingRequest(String requestId, String deployId, long timestamp, Optional<String> user, PendingType pendingType) {
    this(requestId, deployId, timestamp, user, pendingType, Collections.<String> emptyList(), Optional.<String> absent());
  }

  @JsonCreator
  public SingularityPendingRequest(@JsonProperty("requestId") String requestId, @JsonProperty("deployId") String deployId, @JsonProperty("timestamp") long timestamp,
      @JsonProperty("user") Optional<String> user, @JsonProperty("pendingType") PendingType pendingType, @JsonProperty("cmdLineArgsList") List<String> cmdLineArgsList,
      @JsonProperty("runId") Optional<String> runId) {
    this.requestId = requestId;
    this.deployId = deployId;
    this.timestamp = timestamp;
    this.user = user;
    this.cmdLineArgsList = cmdLineArgsList;
    this.pendingType = pendingType;
    this.runId = runId;
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

  public List<String> getCmdLineArgsList() {
    return cmdLineArgsList;
  }

  @Override
  public String toString() {
    return "SingularityPendingRequest [requestId=" + requestId + ", deployId=" + deployId + ", timestamp=" + timestamp + ", pendingType=" + pendingType + ", user=" + user + ", cmdLineArgsList="
        + cmdLineArgsList + "]";
  }

}
