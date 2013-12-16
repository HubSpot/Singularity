package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityPendingRequestId.PendingType;

public class SingularityPendingTaskId extends SingularityId implements Comparable<SingularityPendingTaskId> {

  private final String requestId;
  private final long nextRunAt;
  private final int instanceNo;
  private final PendingType pendingType;

  @JsonCreator
  public SingularityPendingTaskId(@JsonProperty("requestId") String requestId, @JsonProperty("nextRunAt") long nextRunAt, @JsonProperty("instanceNo") int instanceNo, @JsonProperty("pendingType") String pendingType) {
    this(requestId, nextRunAt, instanceNo, PendingType.valueOf(pendingType));
  }
  
  public SingularityPendingTaskId(String requestId, long nextRunAt, int instanceNo, PendingType pendingType) {
    this.requestId = requestId;
    this.nextRunAt = nextRunAt;
    this.instanceNo = instanceNo;
    this.pendingType = pendingType;
  }

  public static List<SingularityPendingTaskId> filter(List<SingularityPendingTaskId> taskIds, String requestId) {
    List<SingularityPendingTaskId> matching = Lists.newArrayList();
    for (SingularityPendingTaskId taskId : taskIds) {
      if (taskId.getRequestId().equals(requestId)) {
        matching.add(taskId);
      }
    }
    return matching;
  }
  
  public String getRequestId() {
    return requestId;
  }

  public long getNextRunAt() {
    return nextRunAt;
  }

  public int getInstanceNo() {
    return instanceNo;
  }
  
  public String getPendingType() {
    return pendingType.name();
  }
  
  @JsonIgnore
  public PendingType getPendingTypeEnum() {
    return pendingType;
  }
    
  public static SingularityPendingTaskId fromString(String string) {
    final String[] splits = JavaUtils.reverseSplit(string, 4, "-");
 
    final String requestId = splits[0];
    final long nextRunAt = Long.parseLong(splits[1]);
    final int instanceNo = Integer.parseInt(splits[2]);
    final PendingType pendingType = PendingType.valueOf(splits[3]);
    
    return new SingularityPendingTaskId(requestId, nextRunAt, instanceNo, pendingType);
  }

  public String toString() {
    return String.format("%s-%s-%s-%s", getRequestId(), getNextRunAt(), getInstanceNo(), getPendingType());
  }
  
  @Override
  public int compareTo(SingularityPendingTaskId o) {
    return ComparisonChain.start()
        .compare(this.getNextRunAt(), o.getNextRunAt())
        .compare(this.getRequestId(), o.getRequestId())
        .compare(this.getInstanceNo(), o.getInstanceNo())
        .compare(this.getPendingType(), o.getPendingType())
        .result();
  }

}
