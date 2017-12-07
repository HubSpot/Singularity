package com.hubspot.singularity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.SingularityMesosArtifact;

public class SingularityPendingTask {

  private final SingularityPendingTaskId pendingTaskId;
  private final Optional<List<String>> cmdLineArgsList;
  private final Optional<String> user;
  private final Optional<String> runId;
  private final Optional<Boolean> skipHealthchecks;
  private final Optional<String> message;
  private final Optional<Resources> resources;
  private final Optional<String> runAsUserOverride;
  private final Map<String, String> envOverrides;
  private final List<SingularityMesosArtifact> extraArtifacts;
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
  public SingularityPendingTask(@JsonProperty("pendingTaskId") SingularityPendingTaskId pendingTaskId,
                                @JsonProperty("cmdLineArgsList") Optional<List<String>> cmdLineArgsList,
                                @JsonProperty("user") Optional<String> user,
                                @JsonProperty("runId") Optional<String> runId,
                                @JsonProperty("skipHealthchecks") Optional<Boolean> skipHealthchecks,
                                @JsonProperty("message") Optional<String> message,
                                @JsonProperty("resources") Optional<Resources> resources,
                                @JsonProperty("runAsUserOverride") Optional<String> runAsUserOverride,
                                @JsonProperty("envOverrides") Map<String, String> envOverrides,
                                @JsonProperty("extraArtifacts") List<SingularityMesosArtifact> extraArtifacts,
                                @JsonProperty("actionId") Optional<String> actionId) {
    this.pendingTaskId = pendingTaskId;
    this.user = user;
    this.message = message;
    this.cmdLineArgsList = cmdLineArgsList;
    this.runId = runId;
    this.skipHealthchecks = skipHealthchecks;
    this.resources = resources;
    this.runAsUserOverride = runAsUserOverride;

    if (Objects.nonNull(envOverrides)) {
      this.envOverrides = envOverrides;
    } else {
      this.envOverrides = Collections.emptyMap();
    }

    if (Objects.nonNull(extraArtifacts)) {
      this.extraArtifacts = extraArtifacts;
    } else {
      this.extraArtifacts = Collections.emptyList();
    }

    this.actionId = actionId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityPendingTask that = (SingularityPendingTask) o;
    return Objects.equals(pendingTaskId, that.pendingTaskId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pendingTaskId);
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

  public Optional<String> getRunAsUserOverride() {
    return runAsUserOverride;
  }

  public Map<String, String> getEnvOverrides() { return envOverrides; }

  public List<SingularityMesosArtifact> getExtraArtifacts() {
    return extraArtifacts;
  }

  public Optional<String> getActionId() {
    return actionId;
  }

  @Override
  public String toString() {
    return "SingularityPendingTask{" +
        "pendingTaskId=" + pendingTaskId +
        ", cmdLineArgsList=" + cmdLineArgsList +
        ", user=" + user +
        ", runId=" + runId +
        ", skipHealthchecks=" + skipHealthchecks +
        ", message=" + message +
        ", resources=" + resources +
        ", runAsUserOverride=" + runAsUserOverride +
        ", envOverrides=" + envOverrides +
        ", extraArtifacts" + extraArtifacts +
        ", actionId=" + actionId +
        '}';
  }
}
