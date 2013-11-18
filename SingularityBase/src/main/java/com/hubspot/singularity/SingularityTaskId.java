package com.hubspot.singularity;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.hubspot.mesos.JavaUtils;

public class SingularityTaskId {

  private final String requestId;
  private final long startedAt;
  private final int instanceNo;
  private final String slave;
  private final String rackId;

  @JsonCreator
  public SingularityTaskId(@JsonProperty("requestId") String requestId, @JsonProperty("nextRunAt") long startedAt, @JsonProperty("instanceNo") int instanceNo, @JsonProperty("slave") String slave, @JsonProperty("rackId") String rackId) {
    this.requestId = requestId;
    this.startedAt = startedAt;
    this.instanceNo = instanceNo;
    this.rackId = rackId;
    this.slave = slave;
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
  
  public String getSlave() {
    return slave;
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
    final String slave = splits[3];
    final String rackId = splits[4];
    
    return new SingularityTaskId(requestId, startedAt, instanceNo, slave, rackId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(instanceNo, requestId, rackId, slave, startedAt);
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
    if (slave == null) {
      if (other.slave != null)
        return false;
    } else if (!slave.equals(other.slave))
      return false;
    if (startedAt != other.startedAt)
      return false;
    return true;
  }
  
  public String toString() {
    return String.format("%s-%s-%s-%s-%s", getRequestId(), getStartedAt(), getInstanceNo(), getSlave(), getRackId());
  }
  
}
