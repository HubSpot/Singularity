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

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Overrides and settings for a particular task being launched")
public class SingularityPendingTask {

  private final SingularityPendingTaskId pendingTaskId;
  private final Optional<List<String>> cmdLineArgsList;
  private final Optional<String> user;
  private final Optional<String> runId;
  private final Optional<Boolean> skipHealthchecks;
  private final Optional<String> message;
  private final Optional<Resources> resources;
  private final List<SingularityS3UploaderFile> s3UploaderAdditionalFiles;
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
                                @JsonProperty("s3UploaderAdditionalFiles") List<SingularityS3UploaderFile> s3UploaderAdditionalFiles,
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

    if (Objects.nonNull(s3UploaderAdditionalFiles)) {
      this.s3UploaderAdditionalFiles = s3UploaderAdditionalFiles;
    } else {
      this.s3UploaderAdditionalFiles = Collections.emptyList();
    }

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

  @Schema(nullable = true, description = "The user who triggered the launch of this task")
  public Optional<String> getUser() {
    return user;
  }

  @Schema(description = "A unique id for the request to launch this task")
  public SingularityPendingTaskId getPendingTaskId() {
    return pendingTaskId;
  }

  @Schema(description = "Extra command line arguments for this particular task")
  public Optional<List<String>> getCmdLineArgsList() {
    return cmdLineArgsList;
  }

  @Schema(nullable = true, description = "An optional unique run id associated with this task")
  public Optional<String> getRunId() {
    return runId;
  }

  @Schema(
      nullable = true,
      title = "If `true`, do not run healthchecks for this task and immediately consider it healthy",
      defaultValue = "false"
  )
  public Optional<Boolean> getSkipHealthchecks() {
    return skipHealthchecks;
  }

  @Schema(description = "An optional message for the launch of this task")
  public Optional<String> getMessage() {
    return message;
  }

  @Schema(
      title = "Optional overrides to the resources requested for this task",
      defaultValue = "resources sepcified in the deploy associated with this task"
  )
  public Optional<Resources> getResources() {
    return resources;
  }

  @Schema(description = "A list of additional files for the SingularityS3Uploader to upload")
  public List<SingularityS3UploaderFile> getS3UploaderAdditionalFiles() {
    return s3UploaderAdditionalFiles;
  }

  @Schema(description = "Override the system user this task will be run as", nullable = true)
  public Optional<String> getRunAsUserOverride() {
    return runAsUserOverride;
  }

  @Schema(description = "Environment variable overrides for this particular task")
  public Map<String, String> getEnvOverrides() { return envOverrides; }

  @Schema(description = "A list of additional artifacts to download for this particular task")
  public List<SingularityMesosArtifact> getExtraArtifacts() {
    return extraArtifacts;
  }

  @Schema(description = "An optional unique id associated with the launch of this task", nullable = true)
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
        ", s3UploaderAdditionalFiles=" + s3UploaderAdditionalFiles +
        ", runAsUserOverride=" + runAsUserOverride +
        ", envOverrides=" + envOverrides +
        ", extraArtifacts" + extraArtifacts +
        ", actionId=" + actionId +
        '}';
  }
}
