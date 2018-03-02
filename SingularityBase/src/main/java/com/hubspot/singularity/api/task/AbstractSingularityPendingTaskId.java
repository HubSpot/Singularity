package com.hubspot.singularity.api.task;

import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;

import com.google.common.base.Predicate;
import com.google.common.collect.ComparisonChain;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.annotations.SingularityStyle;
import com.hubspot.singularity.api.common.SingularityId;
import com.hubspot.singularity.api.request.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.exceptions.InvalidSingularityTaskIdException;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "A unique id describing a task that is waiting to launch")
public abstract class AbstractSingularityPendingTaskId extends SingularityId implements Comparable<SingularityPendingTaskId> {
  public static Predicate<SingularityPendingTaskId> matchingRequestId(final String requestId) {
    return (input) -> input.getRequestId().equals(requestId);
  }

  public static Predicate<SingularityPendingTaskId> matchingDeployId(final String deployId) {
    return (input) -> input.getDeployId().equals(deployId);
  }

  @Derived
  public String getId() {
    return String.format("%s-%s-%s-%s-%s-%s", getRequestId(), getDeployId(), getNextRunAt(), getInstanceNo(), getPendingType(), getCreatedAt());
  }

  @Schema(description = "The request associated with this task")
  public abstract String getRequestId();

  @Schema(description = "The deploy associated with this task")
  public abstract String getDeployId();

  @Schema(description = "The time at which this task should be launched")
  public abstract long getNextRunAt();

  @Schema(description = "The instance number for this task", minimum = "1")
  public abstract int getInstanceNo();

  @Schema(description = "The reason this task was requested to launch")
  public abstract PendingType getPendingType();

  @Schema(description = "The time the pending task was created")
  public abstract long getCreatedAt();


  public static SingularityPendingTaskId valueOf(String string) {
    String[] splits = null;

    try {
      splits = JavaUtils.reverseSplit(string, 6, "-");
    } catch (IllegalStateException ise) {
      throw new InvalidSingularityTaskIdException(String.format("PendingTaskId %s was invalid (%s)", string, ise.getMessage()));
    }

    try {
      return SingularityPendingTaskId.builder()
          .setRequestId(splits[0])
          .setDeployId(splits[1])
          .setNextRunAt(Long.parseLong(splits[2]))
          .setInstanceNo(Integer.parseInt(splits[3]))
          .setPendingType(PendingType.valueOf(splits[4]))
          .setCreatedAt(Long.parseLong(splits[5]))
          .build();
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
