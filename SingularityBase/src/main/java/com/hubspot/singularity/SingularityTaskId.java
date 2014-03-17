package com.hubspot.singularity;

import java.util.List;

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
  
  public static SingularityTaskId fromString(String string) throws InvalidSingularityTaskIdException {
    final String[] splits = JavaUtils.reverseSplit(string, 5, "-");
    
    if (splits.length != 5) {
      throw new InvalidSingularityTaskIdException(String.format("TaskId %s should had split length of %s (instead of 5)", string, splits.length));
    }
    
    try {
      final String requestId = splits[0];
      final long startedAt = Long.parseLong(splits[1]);
      final int instanceNo = Integer.parseInt(splits[2]);
      final String host = splits[3];
      final String rackId = splits[4];
      
      return new SingularityTaskId(requestId, startedAt, instanceNo, host, rackId);
    } catch (NumberFormatException nfe) {
      throw new InvalidSingularityTaskIdException(String.format("TaskId %s had an invalid number parameter (%s)", string, nfe.getMessage()));
    }
  }

  public String toString() {
    return String.format("%s-%s-%s-%s-%s", getRequestId(), getStartedAt(), getInstanceNo(), getHost(), getRackId());
  }
  
}
