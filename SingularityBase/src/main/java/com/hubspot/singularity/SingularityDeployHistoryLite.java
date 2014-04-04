package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityDeployHistoryLite {
  
  private final String requestId;
  private final String deployId;
  private final long timestamp;
  private final Optional<String> user;
  private final Optional<DeployState> deployState;
  
  @JsonCreator
  public SingularityDeployHistoryLite(@JsonProperty("requestId") String requestId, @JsonProperty("deployId") String deployId, @JsonProperty("timestamp") long timestamp, @JsonProperty("user") Optional<String> user, @JsonProperty("deployState") Optional<DeployState> deployState) {
    this.requestId = requestId;
    this.deployId = deployId;
    this.timestamp = timestamp;
    this.user = user;
    this.deployState = deployState;
  }

  public String getRequestId() {
    return requestId;
  }

  public String getDeployId() {
    return deployId;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public Optional<String> getUser() {
    return user;
  }

  public Optional<DeployState> getDeployState() {
    return deployState;
  }

  @Override
  public String toString() {
    return "SingularityDeployHistoryLite [requestId=" + requestId + ", deployId=" + deployId + ", timestamp=" + timestamp + ", user=" + user + ", deployState=" + deployState + "]";
  }
  
}
