package com.hubspot.singularity.api.deploy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Custom executor settings")
public interface ExecutorDataBase {
  @Schema(required = true, description = "Command for the custom executor to run")
  String getCmd();

  @Schema()
  Optional<String> getLoggingTag();

  @Schema()
  Map<String, String> getLoggingExtraFields();

  @Schema(description = "A list of the full content of any embedded artifacts")
  List<EmbeddedArtifact> getEmbeddedArtifacts();

  @Schema(description = "A list of external artifacts for the executor to download")
  List<ExternalArtifact> getExternalArtifacts();

  @Schema(description = "Allowable exit codes for the task to be considered FINISHED instead of FAILED")
  List<Integer> getSuccessfulExitCodes();

  @Schema(description = "Extra arguments in addition to any provided in the cmd field")
  List<String> getExtraCmdLineArgs();

  @Schema()
  Optional<String> getRunningSentinel();

  @Schema(description = "Run the task process as this user")
  Optional<String> getUser();

  @Schema(description = "Send a sigkill to a process if it has not shut down this many millis after being sent a term signal")
  Optional<Long> getSigKillProcessesAfterMillis();

  @Schema(description = "List of s3 artifacts for the executor to download")
  List<S3Artifact> getS3Artifacts();

  @Schema(description = "Maximum number of threads a task is allowed to use")
  Optional<Integer> getMaxTaskThreads();

  @Schema(description = "If true, do not delete files in the task sandbox after the task process has terminated")
  Optional<Boolean> getPreserveTaskSandboxAfterFinish();

  @Schema(description = "Maximum number of open files the task process is allowed")
  Optional<Integer> getMaxOpenFiles();

  @Schema(description = "If true, do not run logrotate or compress old log files")
  Optional<Boolean> getSkipLogrotateAndCompress();

  @Schema(description = "A list of signatures use to verify downloaded s3artifacts")
  Optional<List<S3ArtifactSignature>> getS3ArtifactSignatures();

  @JsonIgnore
  default List<S3ArtifactSignature> getS3ArtifactSignaturesOrEmpty() {
    return getS3ArtifactSignatures().orElse(Collections.emptyList());
  }

  @Schema(description = "Run logrotate this often. Can be HOURLY, DAILY, WEEKLY, MONTHLY")
  Optional<SingularityExecutorLogrotateFrequency> getLogrotateFrequency();
}
