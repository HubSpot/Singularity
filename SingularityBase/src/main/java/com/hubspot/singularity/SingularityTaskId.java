package com.hubspot.singularity;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.hubspot.mesos.JavaUtils;

public class SingularityTaskId extends SingularityId {

  private final String requestId;
  private final String deployId;
  private final long startedAt;
  private final int instanceNo;
  private final String host;
  private final String rackId;

  public static Predicate<SingularityTaskId> matchingRequest(final String requestId) {
    return new Predicate<SingularityTaskId>() {

      @Override
      public boolean apply(@Nonnull SingularityTaskId input) {
        return input.getRequestId().equals(requestId);
      }

    };
  }

  public static Predicate<SingularityTaskId> matchingDeploy(final String deployId) {
    return new Predicate<SingularityTaskId>() {

      @Override
      public boolean apply(@Nonnull SingularityTaskId input) {
        return input.getDeployId().equals(deployId);
      }

    };
  }

  public static Predicate<SingularityTaskId> notIn(Collection<SingularityTaskId> exclude) {
    return Predicates.not(Predicates.in(exclude));
  }

  @SuppressWarnings("unchecked")
  public static List<SingularityTaskId> matchingAndNotIn(Collection<SingularityTaskId> taskIds, String requestId, String deployId, Collection<SingularityTaskId> exclude) {
    return Lists.newArrayList(Iterables.filter(taskIds, Predicates.and(matchingRequest(requestId), matchingDeploy(deployId), notIn(exclude))));
  }

  public static List<SingularityTaskId> matchingAndNotIn(Collection<SingularityTaskId> taskIds, String requestId, Collection<SingularityTaskId> exclude) {
    return Lists.newArrayList(Iterables.filter(taskIds, Predicates.and(matchingRequest(requestId), notIn(exclude))));
  }

  @JsonCreator
  public SingularityTaskId(@JsonProperty("requestId") String requestId, @JsonProperty("deployId") String deployId, @JsonProperty("nextRunAt") long startedAt, @JsonProperty("instanceNo") int instanceNo, @JsonProperty("host") String host, @JsonProperty("rackId") String rackId) {
    this.requestId = requestId;
    this.deployId = deployId;
    this.startedAt = startedAt;
    this.instanceNo = instanceNo;
    this.rackId = rackId;
    this.host = host;
  }

  public String getRackId() {
    return rackId;
  }

  public String getDeployId() {
    return deployId;
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

  public static SingularityTaskId valueOf(String string) throws InvalidSingularityTaskIdException {
    String[] splits = null;

    try {
      splits = JavaUtils.reverseSplit(string, 6, "-");
    } catch (IllegalStateException ise) {
      throw new InvalidSingularityTaskIdException(String.format("TaskId %s was invalid (%s)", string, ise.getMessage()));
    }

    try {
      final String requestId = splits[0];
      final String deployId = splits[1];
      final long startedAt = Long.parseLong(splits[2]);
      final int instanceNo = Integer.parseInt(splits[3]);
      final String host = splits[4];
      final String rackId = splits[5];

      return new SingularityTaskId(requestId, deployId, startedAt, instanceNo, host, rackId);
    } catch (IllegalArgumentException e) {
      throw new InvalidSingularityTaskIdException(String.format("TaskId %s had an invalid parameter (%s)", string, e.getMessage()));
    }
  }

  @Override
  public String getId() {
    return String.format("%s-%s-%s-%s-%s-%s", getRequestId(), getDeployId(), getStartedAt(), getInstanceNo(), getHost(), getRackId());
  }

  @Override
  public String toString() {
    return getId();
  }
}
