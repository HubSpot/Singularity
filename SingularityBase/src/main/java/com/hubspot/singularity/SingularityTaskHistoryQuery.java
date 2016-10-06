package com.hubspot.singularity;

import java.util.Comparator;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ComparisonChain;

public class SingularityTaskHistoryQuery {

  private final Optional<String> requestId;
  private final Optional<String> deployId;
  private final Optional<String> runId;
  private final Optional<String> host;
  private final Optional<ExtendedTaskState> lastTaskStatus;
  private final Optional<Long> startedBefore;
  private final Optional<Long> startedAfter;
  private final Optional<Long> updatedBefore;
  private final Optional<Long> updatedAfter;
  private final Optional<OrderDirection> orderDirection;

  public SingularityTaskHistoryQuery(String requestId) {
    this(Optional.of(requestId), Optional.<String> absent(), Optional.<String> absent(), Optional.<String>absent(), Optional.<ExtendedTaskState> absent(), Optional.<Long> absent(), Optional.<Long> absent(),
        Optional.<Long>absent(), Optional.<Long>absent(), Optional.<OrderDirection> absent());
  }

  public SingularityTaskHistoryQuery(Optional<String> requestId, Optional<String> deployId,  Optional<String> runId, Optional<String> host, Optional<ExtendedTaskState> lastTaskStatus, Optional<Long> startedBefore,
      Optional<Long> startedAfter, Optional<Long> updatedBefore, Optional<Long> updatedAfter, Optional<OrderDirection> orderDirection) {
    this.requestId = requestId;
    this.deployId = deployId;
    this.runId = runId;
    this.host = host;
    this.lastTaskStatus = lastTaskStatus;
    this.startedBefore = startedBefore;
    this.startedAfter = startedAfter;
    this.updatedBefore = updatedBefore;
    this.updatedAfter = updatedAfter;
    this.orderDirection = orderDirection;
  }

  public Optional<String> getRequestId() {
    return requestId;
  }

  public Optional<String> getDeployId() {
    return deployId;
  }

  public Optional<String> getRunId() {
    return runId;
  }

  public Optional<String> getHost() {
    return host;
  }

  public Optional<ExtendedTaskState> getLastTaskStatus() {
    return lastTaskStatus;
  }

  public Optional<Long> getStartedBefore() {
    return startedBefore;
  }

  public Optional<Long> getStartedAfter() {
    return startedAfter;
  }

  public Optional<Long> getUpdatedBefore() {
    return updatedBefore;
  }

  public Optional<Long> getUpdatedAfter() {
    return updatedAfter;
  }

  public Optional<OrderDirection> getOrderDirection() {
    return orderDirection;
  }

  public Predicate<SingularityTaskIdHistory> getHistoryFilter() {
    return new Predicate<SingularityTaskIdHistory>() {

      @Override
      public boolean apply(SingularityTaskIdHistory input) {
        final SingularityTaskId taskId = input.getTaskId();

        if (requestId.isPresent() && !requestId.get().equals(taskId.getRequestId())) {
          return false;
        }

        if (host.isPresent() && !host.get().equals(taskId.getHost())) {
          return false;
        }

        if (deployId.isPresent() && !deployId.get().equals(taskId.getDeployId())) {
          return false;
        }

        if (runId.isPresent() && !runId.get().equals(input.getRunId().or(""))) {
          return false;
        }

        if (lastTaskStatus.isPresent()) {
          if (!input.getLastTaskState().isPresent()) {
            return false;
          }

          if (lastTaskStatus.get() != input.getLastTaskState().get()) {
            return false;
          }
        }

        if (startedAfter.isPresent() && startedAfter.get() >= taskId.getStartedAt()) {
          return false;
        }

        if (startedBefore.isPresent() && startedBefore.get() <= taskId.getStartedAt()) {
          return false;
        }

        if (updatedAfter.isPresent() && updatedAfter.get() >= input.getUpdatedAt()) {
          return false;
        }

        if (updatedBefore.isPresent() && updatedBefore.get() <= input.getUpdatedAt()) {
          return false;
        }

        return true;
      }

    };
  }

  public Comparator<SingularityTaskIdHistory> getComparator() {
    final OrderDirection localOrderDirection = orderDirection.or(OrderDirection.DESC);

    return new Comparator<SingularityTaskIdHistory>() {

      @Override
      public int compare(SingularityTaskIdHistory o1, SingularityTaskIdHistory o2) {
        ComparisonChain chain = ComparisonChain.start();

        if (localOrderDirection == OrderDirection.ASC) {
          chain = chain.compare(o1.getTaskId().getStartedAt(), o2.getTaskId().getStartedAt());
        } else {
          chain = chain.compare(o2.getTaskId().getStartedAt(), o1.getTaskId().getStartedAt());
        }

        return chain.compare(o1.getTaskId().getRequestId(), o2.getTaskId().getRequestId()).result();
      }

    };
  }

  @Override
  public String toString() {
    return "SingularityTaskHistoryQuery [requestId=" + requestId + ", deployId=" + deployId + ", runId=" + runId + ", host=" + host + ", lastTaskStatus=" + lastTaskStatus + ", startedBefore=" + startedBefore
        + ", startedAfter=" + startedAfter + ", orderDirection=" + orderDirection + "]";
  }


}
