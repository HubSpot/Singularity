package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Predicate;
import com.google.common.collect.ComparisonChain;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;

public class SingularityPendingTaskId extends SingularityId implements Comparable<SingularityPendingTaskId> {

  private final String requestId;
  private final String deployId;
  private final long nextRunAt;
  private final int instanceNo;
  private final PendingType pendingType;

  public static Predicate<SingularityPendingTaskId> matchingRequestId(final String requestId) {
    return new Predicate<SingularityPendingTaskId>() {

      @Override
      public boolean apply(SingularityPendingTaskId input) {
        return input.getRequestId().equals(requestId);
      }

    };
  }

  public static Predicate<SingularityPendingTaskId> matchingDeployId(final String deployId) {
    return new Predicate<SingularityPendingTaskId>() {

      @Override
      public boolean apply(SingularityPendingTaskId input) {
        return input.getDeployId().equals(deployId);
      }

    };
  }


  @JsonCreator
  public SingularityPendingTaskId(@JsonProperty("requestId") String requestId, @JsonProperty("deployId") String deployId, @JsonProperty("nextRunAt") long nextRunAt, @JsonProperty("instanceNo") int instanceNo, @JsonProperty("pendingType") PendingType pendingType) {
    this.requestId = requestId;
    this.deployId = deployId;
    this.nextRunAt = nextRunAt;
    this.instanceNo = instanceNo;
    this.pendingType = pendingType;
  }

  public String getDeployId() {
    return deployId;
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

  public PendingType getPendingType() {
    return pendingType;
  }

  public static SingularityPendingTaskId fromString(String string) {
    String[] splits = null;
    
    try {
      splits = JavaUtils.reverseSplit(string, 5, "-");
    } catch (IllegalStateException ise) {
      throw new InvalidSingularityTaskIdException(String.format("PendingTaskId %s was invalid (%s)", string, ise.getMessage()));
    }

    try {
      final String requestId = splits[0];
      final String deployId = splits[1];
      final long nextRunAt = Long.parseLong(splits[2]);
      final int instanceNo = Integer.parseInt(splits[3]);
      final PendingType pendingType = PendingType.valueOf(splits[4]);

      return new SingularityPendingTaskId(requestId, deployId, nextRunAt, instanceNo, pendingType);
    } catch (IllegalArgumentException e) {
      throw new InvalidSingularityTaskIdException(String.format("PendingTaskId %s had an invalid parameter (%s)", string, e.getMessage()));
    }
    
  }

  @Override
  public String toString() {
    return String.format("%s-%s-%s-%s-%s", getRequestId(), getDeployId(), getNextRunAt(), getInstanceNo(), getPendingType());
  }

  @Override
  public int compareTo(SingularityPendingTaskId o) {
    return ComparisonChain.start()
        .compare(this.getNextRunAt(), o.getNextRunAt())
        .compare(this.getRequestId(), o.getRequestId())
        .compare(this.getDeployId(), o.getDeployId())
        .compare(this.getInstanceNo(), o.getInstanceNo())
        .compare(this.getPendingType(), o.getPendingType())
        .result();
  }

}
