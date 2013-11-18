package com.hubspot.singularity;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.hubspot.mesos.JavaUtils;

public class SingularityTaskId extends SingularityId {

  private final String requestId;
  private final long startedAt;
  private final int instanceNo;
  private final String host;
  private final String rackId;

  @JsonCreator
  public SingularityTaskId(@JsonProperty("requestId") String requestId, @JsonProperty("nextRunAt") long startedAt, @JsonProperty("instanceNo") int instanceNo, @JsonProperty("host") String host, @JsonProperty("rackId") String rackId) {
    this.requestId = requestId;
    this.startedAt = startedAt;
    this.instanceNo = instanceNo;
    this.rackId = rackId;
    this.host = host;
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
  
  public String getHost() {
    return host;
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
    final String[] splits = JavaUtils.reverseSplit(string, 5, "-");
    
    final String requestId = splits[0];
    final long startedAt = Long.parseLong(splits[1]);
    final int instanceNo = Integer.parseInt(splits[2]);
    final String host = splits[3];
    final String rackId = splits[4];
    
    return new SingularityTaskId(requestId, startedAt, instanceNo, host, rackId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(instanceNo, requestId, rackId, host, startedAt);
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
    if (rackId == null) {
      if (other.rackId != null)
        return false;
    } else if (!rackId.equals(other.rackId))
      return false;
    if (requestId == null) {
      if (other.requestId != null)
        return false;
    } else if (!requestId.equals(other.requestId))
      return false;
    if (host == null) {
      if (other.host != null)
        return false;
    } else if (!host.equals(other.host))
      return false;
    if (startedAt != other.startedAt)
      return false;
    return true;
  }
  
  public String toString() {
    return String.format("%s-%s-%s-%s-%s", getRequestId(), getStartedAt(), getInstanceNo(), getHost(), getRackId());
  }
  
}
