package com.hubspot.singularity;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Predicate;
import com.google.common.collect.ComparisonChain;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A unique id describing a task that is waiting to launch")
public class SingularityPendingTaskId extends SingularityId implements Comparable<SingularityPendingTaskId> {

  private final String requestId;
  private final String deployId;
  private final long nextRunAt;
  private final long createdAt;
  private final int instanceNo;
  private final PendingType pendingType;

  public static Predicate<SingularityPendingTaskId> matchingRequestId(final String requestId) {
    return new Predicate<SingularityPendingTaskId>() {

      @Override
      public boolean apply(@Nonnull SingularityPendingTaskId input) {
        return input.getRequestId().equals(requestId);
      }

    };
  }

  public static Predicate<SingularityPendingTaskId> matchingDeployId(final String deployId) {
    return new Predicate<SingularityPendingTaskId>() {

      @Override
      public boolean apply(@Nonnull SingularityPendingTaskId input) {
        return input.getDeployId().equals(deployId);
      }

    };
  }

  @JsonCreator
  public SingularityPendingTaskId(@JsonProperty("requestId") String requestId, @JsonProperty("deployId") String deployId, @JsonProperty("nextRunAt") long nextRunAt,
      @JsonProperty("instanceNo") int instanceNo, @JsonProperty("pendingType") PendingType pendingType, @JsonProperty("createdAt") long createdAt) {
    super(String.format("%s-%s-%s-%s-%s-%s", requestId, deployId, nextRunAt, instanceNo, pendingType, createdAt));
    this.requestId = requestId;
    this.deployId = deployId;
    this.nextRunAt = nextRunAt;
    this.createdAt = createdAt;
    this.instanceNo = instanceNo;
    this.pendingType = pendingType;
  }

  @Schema(description = "The deploy associated with this task")
  public String getDeployId() {
    return deployId;
  }

  @Schema(description = "The request associated with this task")
  public String getRequestId() {
    return requestId;
  }

  @Schema(description = "The time at which this task should be launched")
  public long getNextRunAt() {
    return nextRunAt;
  }

  @Schema(description = "The instance number for this task", minimum = "1")
  public int getInstanceNo() {
    return instanceNo;
  }

  @Schema(description = "The time the pending task was created")
  public long getCreatedAt() {
    return createdAt;
  }

  @Schema(description = "The reason this task was requested to launch")
  public PendingType getPendingType() {
    return pendingType;
  }

  public static SingularityPendingTaskId valueOf(String string) {
    String[] splits = null;

    try {
      splits = JavaUtils.reverseSplit(string, 6, "-");
    } catch (IllegalStateException ise) {
      throw new InvalidSingularityTaskIdException(String.format("PendingTaskId %s was invalid (%s)", string, ise.getMessage()));
    }

    try {
      final String requestId = splits[0];
      final String deployId = splits[1];
      final long nextRunAt = Long.parseLong(splits[2]);
      final int instanceNo = Integer.parseInt(splits[3]);
      final PendingType pendingType = PendingType.valueOf(splits[4]);
      final long createdAt = Long.parseLong(splits[5]);

      return new SingularityPendingTaskId(requestId, deployId, nextRunAt, instanceNo, pendingType, createdAt);
    } catch (IllegalArgumentException e) {
      throw new InvalidSingularityTaskIdException(String.format("PendingTaskId %s had an invalid parameter (%s)", string, e.getMessage()));
    }

  }

  @Override
  public String toString() {
    return getId();
  }

  @Override
  public int compareTo(SingularityPendingTaskId o) {
    return ComparisonChain.start()
        .compare(this.getNextRunAt(), o.getNextRunAt())
        .compare(this.getRequestId(), o.getRequestId())
        .compare(this.getDeployId(), o.getDeployId())
        .compare(this.getInstanceNo(), o.getInstanceNo())
        .compare(this.getCreatedAt(), o.getCreatedAt())
        .compare(this.getPendingType(), o.getPendingType())
        .result();
  }

}
