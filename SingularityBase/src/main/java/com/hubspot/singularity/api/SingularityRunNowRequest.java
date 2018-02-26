package com.hubspot.singularity.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.SingularityMesosArtifact;
import com.hubspot.singularity.SingularityS3UploaderFile;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Settings for a specific run of a request")
public class SingularityRunNowRequest {

  private final Optional<String> message;
  private final Optional<String> runId;
  private final Optional<List<String>> commandLineArgs;
  private final Optional<Boolean> skipHealthchecks;
  private final Optional<Resources> resources;
  private final List<SingularityS3UploaderFile> s3UploaderAdditionalFiles;
  private final Optional<String> runAsUserOverride;
  private final Map<String, String> envOverrides;
  private final List<SingularityMesosArtifact> extraArtifacts;
  private final Optional<Long> runAt;

  public SingularityRunNowRequest(
      Optional<String> message,
      Optional<Boolean> skipHealthchecks,
      Optional<String> runId,
      Optional<List<String>> commandLineArgs,
      Optional<Resources> resources
  ) {
    this(message, skipHealthchecks, runId, commandLineArgs, resources, Collections.emptyList(), Optional.absent(), null, null, Optional.absent());
  }

  @Deprecated
  public SingularityRunNowRequest(Optional<String> message,
                                  Optional<Boolean> skipHealthchecks,
                                  Optional<String> runId,
                                  Optional<List<String>> commandLineArgs,
                                  Optional<Resources> resources,
                                  Optional<Long> runAt) {
    this(message, skipHealthchecks, runId, commandLineArgs, resources, Collections.emptyList(), Optional.absent(), null, null, runAt);
  }

  @JsonCreator
  public SingularityRunNowRequest(@JsonProperty("message") Optional<String> message,
                                  @JsonProperty("skipHealthchecks") Optional<Boolean> skipHealthchecks,
                                  @JsonProperty("runId") Optional<String> runId,
                                  @JsonProperty("commandLineArgs") Optional<List<String>> commandLineArgs,
                                  @JsonProperty("resources") Optional<Resources> resources,
                                  @JsonProperty("s3UploaderAdditionalFiles") List<SingularityS3UploaderFile> s3UploaderAdditionalFiles,
                                  @JsonProperty("runAsUserOverride") Optional<String> runAsUserOverride,
                                  @JsonProperty("envOverrides") Map<String, String> envOverrides,
                                  @JsonProperty("extraArtifacts") List<SingularityMesosArtifact> extraArtifacts,
                                  @JsonProperty("runAt") Optional<Long> runAt) {
    this.message = message;
    this.commandLineArgs = commandLineArgs;
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

    this.runAt = runAt;
  }

  @Schema(description = "A message to show to users about why this action was taken", nullable = true)
  public Optional<String> getMessage() {
    return message;
  }

  @Schema(description = "An id to associate with this request which will be associated with the corresponding launched tasks", nullable = true)
  public Optional<String> getRunId() {
    return runId;
  }

  @Schema(description = "Command line arguments to be passed to the task", nullable = true)
  public Optional<List<String>> getCommandLineArgs() {
    return commandLineArgs;
  }

  @Schema(description = "If set to true, healthchecks will be skipped for this task run", nullable = true)
  public Optional<Boolean> getSkipHealthchecks() {
    return skipHealthchecks;
  }

  @Schema(description = "Override the resources from the active deploy for this run", nullable = true)
  public Optional<Resources> getResources() {
    return resources;
  }

  @Schema(description = "Specify additional sandbox files to upload to S3 for this run")
  public List<SingularityS3UploaderFile> getS3UploaderAdditionalFiles() {
    return s3UploaderAdditionalFiles;
  }

  @Schema(description = "Override the user under which this task's command will be launched", nullable = true)
  public Optional<String> getRunAsUserOverride() {
    return runAsUserOverride;
  }

  @Schema(description = "Override the environment variables for launched tasks")
  public Map<String, String> getEnvOverrides() {
    return envOverrides;
  }

  @Schema(description = "Additional artifacts to download for this run")
  public List<SingularityMesosArtifact> getExtraArtifacts() {
    return extraArtifacts;
  }

  @Schema(description = "Schedule this task to run at a specified time", nullable = true)
  public Optional<Long> getRunAt() {
    return runAt;
  }

  @Override
  public String toString() {
    return "SingularityRunNowRequest{" +
        "message=" + message +
        ", runId=" + runId +
        ", commandLineArgs=" + commandLineArgs +
        ", skipHealthchecks=" + skipHealthchecks +
        ", resources=" + resources +
        ", s3UploaderAdditionalFiles=" + s3UploaderAdditionalFiles +
        ", runAsUserOverride=" + runAsUserOverride +
        ", envOverrides=" + envOverrides +
        ", extraArtifacts=" + extraArtifacts +
        ", runAt=" + runAt +
        '}';
  }
}
