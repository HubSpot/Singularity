package com.hubspot.singularity;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.hubspot.mesos.JavaUtils;

public class SingularityPendingTask {

  private final SingularityPendingTaskId pendingTaskId;
  private final List<String> cmdLineArgsList;
  private final Optional<String> user;
  private final Optional<String> runId;

  public static Predicate<SingularityPendingTask> matchingRequest(final String requestId) {
    return new Predicate<SingularityPendingTask>() {

      @Override
      public boolean apply(@Nonnull SingularityPendingTask input) {
        return input.getPendingTaskId().getRequestId().equals(requestId);
      }

    };
  }

  public static Predicate<SingularityPendingTask> matchingDeploy(final String deployId) {
    return new Predicate<SingularityPendingTask>() {

      @Override
      public boolean apply(@Nonnull SingularityPendingTask input) {
        return input.getPendingTaskId().getDeployId().equals(deployId);
      }

    };
  }

  @JsonCreator
  public SingularityPendingTask(@JsonProperty("pendingTaskId") SingularityPendingTaskId pendingTaskId, @JsonProperty("cmdLineArgsList") List<String> cmdLineArgsList,
      @JsonProperty("user") Optional<String> user, @JsonProperty("runId") Optional<String> runId) {
    this.pendingTaskId = pendingTaskId;
    this.user = user;
    this.cmdLineArgsList = JavaUtils.nonNullImmutable(cmdLineArgsList);
    this.runId = runId;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(pendingTaskId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    SingularityPendingTask other = (SingularityPendingTask) obj;
    return Objects.equals(pendingTaskId, other.getPendingTaskId());
  }

  public Optional<String> getUser() {
    return user;
  }

  public SingularityPendingTaskId getPendingTaskId() {
    return pendingTaskId;
  }

  public List<String> getCmdLineArgsList() {
    return cmdLineArgsList;
  }

  public Optional<String> getRunId() {
    return runId;
  }

  @Override
  public String toString() {
    return "SingularityPendingTask [pendingTaskId=" + pendingTaskId + ", cmdLineArgsList=" + cmdLineArgsList + ", user=" + user + ", runId=" + runId + "]";
  }

}
