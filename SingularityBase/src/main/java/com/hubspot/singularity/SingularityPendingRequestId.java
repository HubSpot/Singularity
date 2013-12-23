package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.mesos.JavaUtils;

public class SingularityPendingRequestId extends SingularityId {

  public enum PendingType {
    IMMEDIATE, STARTUP, REGULAR, UNPAUSED, RETRY, BOUNCE
  }
  
  private final String requestId;
  private final PendingType pendingType;
  
  public SingularityPendingRequestId(String requestId) {
    this(requestId, PendingType.REGULAR);
  }
  
  public SingularityPendingRequestId(String requestId, PendingType pendingType) {
    this.requestId = requestId;
    this.pendingType = pendingType;
  }
  
  @JsonCreator
  public SingularityPendingRequestId(@JsonProperty("requestId") String requestId, @JsonProperty("pendingType") String pendingType) {
    this(requestId, PendingType.valueOf(pendingType));
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

  public static SingularityPendingRequestId fromString(String string) {
    final String[] splits = JavaUtils.reverseSplit(string, 2, "-");
    
    return new SingularityPendingRequestId(splits[0], splits[1]);
  }
  
  @Override
  public String toString() {
    return String.format("%s-%s", requestId, pendingType);
  }
  
}
