package com.hubspot.singularity.api.request;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;
import com.hubspot.singularity.api.deploy.mesos.Resources;
import com.hubspot.singularity.api.deploy.mesos.SingularityMesosArtifact;
import com.hubspot.singularity.api.logs.SingularityS3UploaderFile;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "Settings for a specific run of a request")
public interface SingularityRunNowRequestIF {
  @Schema(description = "A message to show to users about why this action was taken", nullable = true)
  Optional<String> getMessage();

  @Schema(description = "If set to true, healthchecks will be skipped for this task run", nullable = true)
  Optional<Boolean> getSkipHealthchecks();

  @Schema(description = "An id to associate with this request which will be associated with the corresponding launched tasks", nullable = true)
  Optional<String> getRunId();

  @Schema(description = "Command line arguments to be passed to the task", nullable = true)
  Optional<List<String>> getCommandLineArgs();


  @Schema(description = "Override the resources from the active deploy for this run", nullable = true)
  Optional<Resources> getResources();

  @Schema(description = "Specify additional sandbox files to upload to S3 for this run")
  List<SingularityS3UploaderFile> getS3UploaderAdditionalFiles();

  @Schema(description = "Override the user under which this task's command will be launched", nullable = true)
  Optional<String> getRunAsUserOverride();

  @Schema(description = "Override the environment variables for launched tasks")
  Map<String, String> getEnvOverrides();

  @Schema(description = "Additional artifacts to download for this run")
  List<SingularityMesosArtifact> getExtraArtifacts();

  @Schema(description = "Schedule this task to run at a specified time", nullable = true)
  Optional<Long> getRunAt();
}
