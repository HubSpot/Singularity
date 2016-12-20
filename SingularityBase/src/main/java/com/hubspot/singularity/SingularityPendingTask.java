package com.hubspot.singularity;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.hubspot.mesos.Resources;

public class SingularityPendingTask {

  private final SingularityPendingTaskId pendingTaskId;
  private final Optional<List<String>> cmdLineArgsList;
  private final Optional<String> user;
  private final Optional<String> runId;
  private final Optional<Boolean> skipHealthchecks;
  private final Optional<String> message;
  private final Optional<Resources> resources;
  private final Optional<String> actionId;

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
  public SingularityPendingTask(@JsonProperty("pendingTaskId") SingularityPendingTaskId pendingTaskId, @JsonProperty("cmdLineArgsList") Optional<List<String>> cmdLineArgsList,
      @JsonProperty("user") Optional<String> user, @JsonProperty("runId") Optional<String> runId, @JsonProperty("skipHealthchecks") Optional<Boolean> skipHealthchecks,
      @JsonProperty("message") Optional<String> message, @JsonProperty("resources") Optional<Resources> resources, @JsonProperty("actionId") Optional<String> actionId) {
    this.pendingTaskId = pendingTaskId;
    this.user = user;
    this.message = message;
    this.cmdLineArgsList = cmdLineArgsList;
    this.runId = runId;
    this.skipHealthchecks = skipHealthchecks;
    this.resources = resources;
    this.actionId = actionId;
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

  public Optional<List<String>> getCmdLineArgsList() {
    return cmdLineArgsList;
  }

  public Optional<String> getRunId() {
    return runId;
  }

  public Optional<Boolean> getSkipHealthchecks() {
    return skipHealthchecks;
  }

  public Optional<String> getMessage() {
    return message;
  }

  public Optional<Resources> getResources() {
    return resources;
  }

  public Optional<String> getActionId() {
    return actionId;
  }

  @Override
  public String toString() {
    return "SingularityPendingTask [pendingTaskId=" + pendingTaskId + ", cmdLineArgsList=" + cmdLineArgsList + ", user=" + user + ", runId=" + runId + ", skipHealthchecks=" + skipHealthchecks
        + ", message=" + message + ", resources=" + resources + ", actionId=" + actionId + "]";
  }

}
