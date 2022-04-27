package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.SingularityMesosArtifact;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
  private final Map<String, String> requiredAgentAttributeOverrides;
  private final Map<String, String> allowedAgentAttributeOverrides;
  private final List<SingularityMesosArtifact> extraArtifacts;
  private final Optional<String> actionId;

  public static Predicate<SingularityPendingTask> matchingRequest(
    final String requestId
  ) {
    return input -> input.getPendingTaskId().getRequestId().equals(requestId);
  }

  public static Predicate<SingularityPendingTask> matchingDeploy(final String deployId) {
    return input -> input.getPendingTaskId().getDeployId().equals(deployId);
  }

  @SuppressFBWarnings("NP_NULL_PARAM_DEREF_NONVIRTUAL")
  public SingularityPendingTask(
    SingularityPendingTaskId pendingTaskId,
    Optional<List<String>> cmdLineArgsList,
    Optional<String> user,
    Optional<String> runId,
    Optional<Boolean> skipHealthchecks,
    Optional<String> message,
    Optional<Resources> resources,
    List<SingularityS3UploaderFile> s3UploaderAdditionalFiles,
    Optional<String> runAsUserOverride,
    Map<String, String> envOverrides,
    Map<String, String> requiredAgentAttributeOverrides,
    Map<String, String> allowedAgentAttributeOverrides,
    List<SingularityMesosArtifact> extraArtifacts,
    Optional<String> actionId
  ) {
    this(
      pendingTaskId,
      cmdLineArgsList,
      user,
      runId,
      skipHealthchecks,
      message,
      resources,
      s3UploaderAdditionalFiles,
      runAsUserOverride,
      envOverrides,
      null,
      null,
      extraArtifacts,
      actionId,
      requiredAgentAttributeOverrides,
      allowedAgentAttributeOverrides
    );
  }

  @JsonCreator
  public SingularityPendingTask(
    @JsonProperty("pendingTaskId") SingularityPendingTaskId pendingTaskId,
    @JsonProperty("cmdLineArgsList") Optional<List<String>> cmdLineArgsList,
    @JsonProperty("user") Optional<String> user,
    @JsonProperty("runId") Optional<String> runId,
    @JsonProperty("skipHealthchecks") Optional<Boolean> skipHealthchecks,
    @JsonProperty("message") Optional<String> message,
    @JsonProperty("resources") Optional<Resources> resources,
    @JsonProperty(
      "s3UploaderAdditionalFiles"
    ) List<SingularityS3UploaderFile> s3UploaderAdditionalFiles,
    @JsonProperty("runAsUserOverride") Optional<String> runAsUserOverride,
    @JsonProperty("envOverrides") Map<String, String> envOverrides,
    @JsonProperty(
      "requiredSlaveAttributeOverrides"
    ) Map<String, String> requiredSlaveAttributeOverrides,
    @JsonProperty(
      "allowedSlaveAttributeOverrides"
    ) Map<String, String> allowedSlaveAttributeOverrides,
    @JsonProperty("extraArtifacts") List<SingularityMesosArtifact> extraArtifacts,
    @JsonProperty("actionId") Optional<String> actionId,
    @JsonProperty(
      "requiredAgentAttributeOverrides"
    ) Map<String, String> requiredAgentAttributeOverrides,
    @JsonProperty(
      "allowedAgentAttributeOverrides"
    ) Map<String, String> allowedAgentAttributeOverrides
  ) {
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

    this.requiredAgentAttributeOverrides =
      MoreObjects.firstNonNull(
        requiredAgentAttributeOverrides,
        MoreObjects.firstNonNull(requiredSlaveAttributeOverrides, Collections.emptyMap())
      );
    this.allowedAgentAttributeOverrides =
      MoreObjects.firstNonNull(
        allowedAgentAttributeOverrides,
        MoreObjects.firstNonNull(allowedSlaveAttributeOverrides, Collections.emptyMap())
      );

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

  @Schema(
    nullable = true,
    description = "An optional unique run id associated with this task"
  )
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

  @Schema(
    description = "A list of additional files for the SingularityS3Uploader to upload"
  )
  public List<SingularityS3UploaderFile> getS3UploaderAdditionalFiles() {
    return s3UploaderAdditionalFiles;
  }

  @Schema(
    description = "Override the system user this task will be run as",
    nullable = true
  )
  public Optional<String> getRunAsUserOverride() {
    return runAsUserOverride;
  }

  @Schema(description = "Environment variable overrides for this particular task")
  public Map<String, String> getEnvOverrides() {
    return envOverrides;
  }

  @Schema(
    description = "Required slave attribute overrides for this particular task. These will be applied on top of any requiredSlaveAttributes that are defined at the SingularityRequest level."
  )
  public Map<String, String> getRequiredAgentAttributeOverrides() {
    return requiredAgentAttributeOverrides;
  }

  @Schema(
    description = "Allowed slave attribute overrides for this particular task. These will be applied on top of any allowedSlaveAttributes that are defined at the SingularityRequest level."
  )
  public Map<String, String> getAllowedAgentAttributeOverrides() {
    return allowedAgentAttributeOverrides;
  }

  @Schema(
    description = "Required slave attribute overrides for this particular task. These will be applied on top of any requiredSlaveAttributes that are defined at the SingularityRequest level."
  )
  @Deprecated
  public Map<String, String> getRequiredSlaveAttributeOverrides() {
    return requiredAgentAttributeOverrides;
  }

  @Schema(
    description = "Allowed slave attribute overrides for this particular task. These will be applied on top of any allowedSlaveAttributes that are defined at the SingularityRequest level."
  )
  @Deprecated
  public Map<String, String> getAllowedSlaveAttributeOverrides() {
    return allowedAgentAttributeOverrides;
  }

  @Schema(
    description = "A list of additional artifacts to download for this particular task"
  )
  public List<SingularityMesosArtifact> getExtraArtifacts() {
    return extraArtifacts;
  }

  @Schema(
    description = "An optional unique id associated with the launch of this task",
    nullable = true
  )
  public Optional<String> getActionId() {
    return actionId;
  }

  @Override
  public String toString() {
    return (
      "SingularityPendingTask{" +
      "pendingTaskId=" +
      pendingTaskId +
      ", cmdLineArgsList=" +
      cmdLineArgsList +
      ", user=" +
      user +
      ", runId=" +
      runId +
      ", skipHealthchecks=" +
      skipHealthchecks +
      ", message=" +
      message +
      ", resources=" +
      resources +
      ", s3UploaderAdditionalFiles=" +
      s3UploaderAdditionalFiles +
      ", runAsUserOverride=" +
      runAsUserOverride +
      ", envOverrides=" +
      envOverrides +
      ", requiredAgentAttributeOverrides=" +
      requiredAgentAttributeOverrides +
      ", allowedAgentAttributeOverrides=" +
      allowedAgentAttributeOverrides +
      ", extraArtifacts" +
      extraArtifacts +
      ", actionId=" +
      actionId +
      '}'
    );
  }
}
