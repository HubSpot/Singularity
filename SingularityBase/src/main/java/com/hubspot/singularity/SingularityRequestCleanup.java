package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityRequestCleanup {

  public enum RequestCleanupType {
    DELETING, PAUSING, BOUNCE, INCREMENTAL_BOUNCE;
  }

  private final Optional<String> user;
  private final RequestCleanupType cleanupType;
  private final Optional<Boolean> killTasks;
  private final Optional<Boolean> skipHealthchecks;
  private final Optional<String> deployId;
  private final long timestamp;
  private final String requestId;

  @JsonCreator
  public SingularityRequestCleanup(@JsonProperty("user") Optional<String> user, @JsonProperty("cleanupType") RequestCleanupType cleanupType, @JsonProperty("timestamp") long timestamp,
      @JsonProperty("killTasks") Optional<Boolean> killTasks, @JsonProperty("requestId") String requestId, @JsonProperty("deployId") Optional<String> deployId,
      @JsonProperty("skipHealthchecks") Optional<Boolean> skipHealthchecks) {
    this.user = user;
    this.cleanupType = cleanupType;
    this.timestamp = timestamp;
    this.requestId = requestId;
    this.deployId = deployId;
    this.killTasks = killTasks;
    this.skipHealthchecks = skipHealthchecks;
  }

  public Optional<Boolean> getSkipHealthchecks() {
    return skipHealthchecks;
  }

  public Optional<Boolean> getKillTasks() {
    return killTasks;
  }

  public String getRequestId() {
    return requestId;
  }

  public Optional<String> getUser() {
    return user;
  }

  public RequestCleanupType getCleanupType() {
    return cleanupType;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public Optional<String> getDeployId() {
    return deployId;
  }

  @Override
  public String toString() {
    return "SingularityRequestCleanup [user=" + user + ", cleanupType=" + cleanupType + ", killTasks=" + killTasks + ", skipHealthchecks=" + skipHealthchecks + ", deployId=" + deployId
        + ", timestamp=" + timestamp + ", requestId=" + requestId + "]";
  }

}
