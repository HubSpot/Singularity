package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityPendingRequest {

  public enum PendingType {
    IMMEDIATE(true), ONEOFF(true), BOUNCE(true), NEW_DEPLOY(false), UNPAUSED(false), RETRY(false), UPDATED_REQUEST(false), DECOMISSIONED_SLAVE_OR_RACK(false), TASK_DONE(false);

    private final boolean hasPriority;

    private PendingType(boolean hasPriority) {
      this.hasPriority = hasPriority;
    }

    public boolean hasPriority() {
      return hasPriority;
    }

  }

  private final String requestId;
  private final String deployId;
  private final long timestamp;
  private final PendingType pendingType;
  private final Optional<String> user;
  private final Optional<String> cmdLineArgs;

  public SingularityPendingRequest(String requestId, String deployId, PendingType pendingType) {
    this(requestId, deployId, System.currentTimeMillis(), Optional.<String> absent(), Optional.<String> absent(), pendingType);
  }

  @JsonCreator
  public SingularityPendingRequest(@JsonProperty("requestId") String requestId, @JsonProperty("deployId") String deployId, @JsonProperty("timestamp") long timestamp, @JsonProperty("cmdLineArgs") Optional<String> cmdLineArgs, @JsonProperty("user") Optional<String> user, @JsonProperty("pendingType") PendingType pendingType) {
    this.requestId = requestId;
    this.deployId = deployId;
    this.timestamp = timestamp;
    this.user = user;
    this.cmdLineArgs = cmdLineArgs;
    this.pendingType = pendingType;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public Optional<String> getCmdLineArgs() {
    return cmdLineArgs;
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

  public boolean hasPriority(SingularityPendingRequest otherRequest) {
    if (pendingType.hasPriority == otherRequest.pendingType.hasPriority) {
      if (timestamp > otherRequest.timestamp) {
        return true;
      }
    } else if (pendingType.hasPriority) {
      return true;
    }

    return false;
  }

  @Override
  public String toString() {
    return "SingularityPendingRequest [requestId=" + requestId + ", deployId=" + deployId + ", timestamp=" + timestamp + ", user=" + user + ", pendingType=" + pendingType + ", cmdLineArgs=" + cmdLineArgs + "]";
  }

}
