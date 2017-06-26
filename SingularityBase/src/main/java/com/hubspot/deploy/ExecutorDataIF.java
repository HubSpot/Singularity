package com.hubspot.deploy;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;
import com.hubspot.singularity.executor.SingularityExecutorLogrotateFrequency;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Immutable
@SingularityStyle
public interface ExecutorDataIF {
  @ApiModelProperty(required=true, value="Command for the custom executor to run")
  String getCmd();

  @ApiModelProperty(required=false, value="A list of the full content of any embedded artifacts")
  List<EmbeddedArtifact> getEmbeddedArtifacts();

  @ApiModelProperty(required=false, value="A list of external artifacts for the executor to download")
  List<ExternalArtifact> getExternalArtifacts();

  @ApiModelProperty(required=false, value="List of s3 artifacts for the executor to download")
  List<S3Artifact> getS3Artifacts();

  @ApiModelProperty(required=false, value="Allowable exit codes for the task to be considered FINISHED instead of FAILED")
  List<Integer> getSuccessfulExitCodes();

  @ApiModelProperty(required=false, value="Run the task process as this user")
  Optional<String> getUser();

  @ApiModelProperty(required=false)
  Optional<String> getRunningSentinel();

  @ApiModelProperty(required=false, value="Extra arguments in addition to any provided in the cmd field")
  List<String> getExtraCmdLineArgs();

  @ApiModelProperty(required=false)
  Optional<String> getLoggingTag();

  @ApiModelProperty(required=false)
  Map<String, String> getLoggingExtraFields();

  @ApiModelProperty(required=false, value="Send a sigkill to a process if it has not shut down this many millis after being sent a term signal")
  Optional<Long> getSigKillProcessesAfterMillis();

  @ApiModelProperty(required=false, value="Maximum number of threads a task is allowed to use")
  Optional<Integer> getMaxTaskThreads();

  @ApiModelProperty(required=false, value="If true, do not delete files in the task sandbox after the task process has terminated")
  Optional<Boolean> getPreserveTaskSandboxAfterFinish();

  @ApiModelProperty(required=false, value="Maximum number of open files the task process is allowed")
  Optional<Integer> getMaxOpenFiles();

  @ApiModelProperty(required=false, value="If true, do not run logrotate or compress old log files")
  Optional<Boolean> getSkipLogrotateAndCompress();

  @ApiModelProperty(required=false, value="A list of signatures use to verify downloaded s3artifacts")
  List<S3ArtifactSignature> getS3ArtifactSignatures();

  @JsonIgnore
  List<S3ArtifactSignature> getS3ArtifactSignaturesOrEmpty();

  @ApiModelProperty(required=false, value="Run logrotate this often. Can be HOURLY, DAILY, WEEKLY, MONTHLY")
  Optional<SingularityExecutorLogrotateFrequency> getLogrotateFrequency();

  static ExecutorData of(String cmd,
                         List<EmbeddedArtifact> embeddedArtifacts,
                         List<ExternalArtifact> externalArtifacts,
                         List<S3Artifact> s3Artifacts,
                         List<Integer> successfulExitCodes,
                         Optional<String> user,
                         Optional<String> runningSentinel,
                         List<String> extraCmdLineArgs,
                         Optional<String> loggingTag,
                         Map<String, String> loggingExtraFields,
                         Optional<Long> sigKillProcessesAfterMillis,
                         Optional<Integer> maxTaskThreads,
                         Optional<Boolean> preserveTaskSandboxAfterFinish,
                         Optional<Integer> maxOpenFiles,
                         Optional<Boolean> skipLogrotateAndCompress,
                         Optional<List<S3ArtifactSignature>> s3ArtifactSignatures,
                         Optional<SingularityExecutorLogrotateFrequency> logrotateFrequency) {
      return ExecutorData.builder()
          .setCmd(cmd)
          .setEmbeddedArtifacts(embeddedArtifacts)
          .setExternalArtifacts(externalArtifacts)
          .setS3Artifacts(s3Artifacts)
          .setSuccessfulExitCodes(successfulExitCodes)
          .setUser(user)
          .setRunningSentinel(runningSentinel)
          .setExtraCmdLineArgs(extraCmdLineArgs)
          .setLoggingTag(loggingTag)
          .setLoggingExtraFields(loggingExtraFields)
          .setSigKillProcessesAfterMillis(sigKillProcessesAfterMillis)
          .setMaxTaskThreads(maxTaskThreads)
          .setPreserveTaskSandboxAfterFinish(preserveTaskSandboxAfterFinish)
          .setMaxOpenFiles(maxOpenFiles)
          .setSkipLogrotateAndCompress(skipLogrotateAndCompress)
          .setS3ArtifactSignatures(s3ArtifactSignatures.or(Collections.emptyList()))
          .setLogrotateFrequency(logrotateFrequency)
          .build();
  }
}
