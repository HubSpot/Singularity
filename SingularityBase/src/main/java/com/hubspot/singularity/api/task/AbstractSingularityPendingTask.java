package com.hubspot.singularity.api.task;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value.Immutable;

import com.google.common.base.Predicate;
import com.hubspot.singularity.annotations.SingularityStyle;
import com.hubspot.singularity.api.deploy.mesos.Resources;
import com.hubspot.singularity.api.deploy.mesos.SingularityMesosArtifact;
import com.hubspot.singularity.api.logs.SingularityS3UploaderFile;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "Overrides and settings for a particular task being launched")
public abstract class AbstractSingularityPendingTask {
  public static Predicate<SingularityPendingTask> matchingRequest(final String requestId) {
    return (input) -> input.getPendingTaskId().getRequestId().equals(requestId);
  }

  public static Predicate<SingularityPendingTask> matchingDeploy(final String deployId) {
    return (input) -> input.getPendingTaskId().getDeployId().equals(deployId);
  }

  @Schema(nullable = true, description = "The user who triggered the launch of this task")
  public abstract Optional<String> getUser();

  @Schema(description = "A unique id for the request to launch this task")
  public abstract SingularityPendingTaskId getPendingTaskId();

  @Schema(description = "Extra command line arguments for this particular task")
  public abstract Optional<List<String>> getCmdLineArgsList();

  @Schema(nullable = true, description = "An optional unique run id associated with this task")
  public abstract Optional<String> getRunId();

  @Schema(
      nullable = true,
      title = "If `true`, do not run healthchecks for this task and immediately consider it healthy",
      defaultValue = "false"
  )
  public abstract Optional<Boolean> getSkipHealthchecks();

  @Schema(description = "An optional message for the launch of this task")
  public abstract Optional<String> getMessage();

  @Schema(
      title = "Optional overrides to the resources requested for this task",
      defaultValue = "resources sepcified in the deploy associated with this task"
  )
  public abstract Optional<Resources> getResources();

  @Schema(description = "A list of additional files for the SingularityS3Uploader to upload")
  public abstract List<SingularityS3UploaderFile> getS3UploaderAdditionalFiles();

  @Schema(description = "Override the system user this task will be run as", nullable = true)
  public abstract Optional<String> getRunAsUserOverride();

  @Schema(description = "Environment variable overrides for this particular task")
  public abstract Map<String, String> getEnvOverrides();

  @Schema(description = "A list of additional artifacts to download for this particular task")
  public abstract List<SingularityMesosArtifact> getExtraArtifacts();

  @Schema(description = "An optional unique id associated with the launch of this task", nullable = true)
  public abstract Optional<String> getActionId();

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityPendingTask that = (SingularityPendingTask) o;
    return Objects.equals(getPendingTaskId(), that.getPendingTaskId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getPendingTaskId());
  }

}
