package com.hubspot.singularity;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;

public class SingularityTaskId {

  private final String requestId;
  private final long startedAt;
  private final int instanceNo;
  private final String rackId;

  @JsonCreator
  public SingularityTaskId(@JsonProperty("requestId") String requestId, @JsonProperty("nextRunAt") long startedAt, @JsonProperty("instanceNo") int instanceNo, @JsonProperty("rackId") String rackId) {
    this.requestId = requestId;
    this.startedAt = startedAt;
    this.instanceNo = instanceNo;
    this.rackId = rackId;
  }
  
  public static List<SingularityTaskId> filter(List<SingularityTaskId> taskIds, String requestId) {
    List<SingularityTaskId> matching = Lists.newArrayList();
    for (SingularityTaskId taskId : taskIds) {
      if (taskId.getRequestId().equals(requestId)) {
        matching.add(taskId);
      }
    }
    return matching;
  }
  
  public String getRackId() {
    return rackId;
  }
  
  @JsonIgnore
  public String getSafeRackId() {
    return rackId.replace("-", "");
  }

  public String getRequestId() {
    return requestId;
  }

  public long getStartedAt() {
    return startedAt;
  }

  public int getInstanceNo() {
    return instanceNo;
  }
  
  public boolean matches(SingularityPendingTaskId pendingTaskId) {
    return getRequestId().equals(pendingTaskId.getRequestId());
  }
  
  public static SingularityTaskId fromString(String string) {
    final String[] splits = string.split("\\-");
    
    final String rackId = splits[splits.length - 1];
    final int instanceNo = Integer.parseInt(splits[splits.length - 2]);
    final long startedAt = Long.parseLong(splits[splits.length - 3]);
    
    StringBuilder requestIdBldr = new StringBuilder();
    
    for (int s = 0; s < splits.length - 3; s++) {
      requestIdBldr.append(splits[s]);
      if (s < splits.length - 4) {
        requestIdBldr.append("-");
      }
    }
     
    final String requestId = requestIdBldr.toString();
    
    return new SingularityTaskId(requestId, startedAt, instanceNo, rackId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(instanceNo, requestId, rackId, startedAt);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SingularityTaskId other = (SingularityTaskId) obj;
    if (instanceNo != other.instanceNo)
      return false;
    if (requestId == null) {
      if (other.requestId != null)
        return false;
    } else if (!requestId.equals(other.requestId))
      return false;
    if (rackId == null) {
      if (other.rackId != null)
        return false;
    } else if (!rackId.equals(other.rackId))
      return false;
    if (startedAt != other.startedAt)
      return false;
    return true;
  }

  public String toString() {
    return String.format("%s-%s-%s-%s", getRequestId(), getStartedAt(), getInstanceNo(), getSafeRackId());
  }
  
}
