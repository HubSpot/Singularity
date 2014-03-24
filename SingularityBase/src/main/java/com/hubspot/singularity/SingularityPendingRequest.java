package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

public class SingularityPendingRequest extends SingularityJsonObject {

  public enum PendingType {
    IMMEDIATE, STARTUP, REGULAR, UNPAUSED, RETRY, BOUNCE, ONEOFF, UPDATED_REQUEST, NEW_REQUEST, DECOMISSIONED_SLAVE_OR_RACK, TASK_DONE, NEW_DEPLOY
  }
  
  private final String requestId;
  private final String deployId;
  private final long timestamp;
  private final PendingType pendingType;
  private final Optional<String> user;
  private final Optional<String> cmdLineArgs;
  
  public static SingularityPendingRequest fromBytes(byte[] bytes, ObjectMapper objectMapper) throws Exception {
    return objectMapper.readValue(bytes, SingularityPendingRequest.class);
  }
  
  @JsonCreator
  public SingularityPendingRequest(@JsonProperty("requestId") String requestId, @JsonProperty("deployId") String deployId, @JsonProperty("timestamp") long timestamp, @JsonProperty("cmdLineArgs") Optional<String> cmdLineArgs, @JsonProperty("user") Optional<String> user, @JsonProperty("pendingType") String pendingType) {
    this(requestId, deployId, timestamp, cmdLineArgs, user, PendingType.valueOf(pendingType));
  }
  
  public SingularityPendingRequest(String requestId, String deployId, PendingType pendingType) {
    this(requestId, deployId, System.currentTimeMillis(), Optional.<String> absent(), Optional.<String> absent(), pendingType);
  }
  
  public SingularityPendingRequest(String requestId, String deployId, long timestamp, Optional<String> cmdLineArgs, Optional<String> user, PendingType pendingType) {
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
  
  @JsonIgnore
  public PendingType getPendingTypeEnum() {
    return pendingType;
  }
  
  public String getPendingType() {
    return pendingType.name();
  }

  @Override
  public String toString() {
    return "SingularityPendingRequest [requestId=" + requestId + ", deployId=" + deployId + ", timestamp=" + timestamp + ", user=" + user + ", pendingType=" + pendingType + ", cmdLineArgs=" + cmdLineArgs + "]";
  }
  
}
