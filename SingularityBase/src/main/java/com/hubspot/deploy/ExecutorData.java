package com.hubspot.deploy;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.executor.SingularityExecutorLogrotateFrequency;
import com.wordnik.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExecutorData {

  private final String cmd;
  private final List<EmbeddedArtifact> embeddedArtifacts;
  private final List<ExternalArtifact> externalArtifacts;
  private final List<S3Artifact> s3Artifacts;
  private final List<Integer> successfulExitCodes;
  private final Optional<String> runningSentinel;
  private final Optional<String> user;
  private final List<String> extraCmdLineArgs;
  private final Optional<String> loggingTag;
  private final Map<String, String> loggingExtraFields;
  private final Optional<Long> sigKillProcessesAfterMillis;
  private final Optional<Integer> maxTaskThreads;
  private final Optional<Boolean> preserveTaskSandboxAfterFinish;
  private final Optional<Integer> maxOpenFiles;
  private final Optional<Boolean> skipLogrotateAndCompress;
  private final Optional<List<S3ArtifactSignature>> s3ArtifactSignatures;
  private final Optional<SingularityExecutorLogrotateFrequency> logrotateFrequency;

  @JsonCreator
  public ExecutorData(@JsonProperty("cmd") String cmd, @JsonProperty("embeddedArtifacts") List<EmbeddedArtifact> embeddedArtifacts, @JsonProperty("externalArtifacts") List<ExternalArtifact> externalArtifacts,
      @JsonProperty("s3Artifacts") List<S3Artifact> s3Artifacts, @JsonProperty("successfulExitCodes") List<Integer> successfulExitCodes, @JsonProperty("user") Optional<String> user,
      @JsonProperty("runningSentinel") Optional<String> runningSentinel, @JsonProperty("extraCmdLineArgs") List<String> extraCmdLineArgs, @JsonProperty("loggingTag") Optional<String> loggingTag,
      @JsonProperty("loggingExtraFields") Map<String, String> loggingExtraFields, @JsonProperty("sigKillProcessesAfterMillis") Optional<Long> sigKillProcessesAfterMillis,
      @JsonProperty("maxTaskThreads") Optional<Integer> maxTaskThreads, @JsonProperty("preserveTaskSandboxAfterFinish") Optional<Boolean> preserveTaskSandboxAfterFinish, @JsonProperty("maxOpenFiles") Optional<Integer> maxOpenFiles,
      @JsonProperty("skipLogrotateAndCompress") Optional<Boolean> skipLogrotateAndCompress, @JsonProperty("s3ArtifactSignatures") Optional<List<S3ArtifactSignature>> s3ArtifactSignatures,
      @JsonProperty("logrotateFrequency") Optional<SingularityExecutorLogrotateFrequency> logrotateFrequency) {
    this.cmd = cmd;
    this.embeddedArtifacts = JavaUtils.nonNullImmutable(embeddedArtifacts);
    this.externalArtifacts = JavaUtils.nonNullImmutable(externalArtifacts);
    this.s3Artifacts = JavaUtils.nonNullImmutable(s3Artifacts);
    this.user = user;
    this.successfulExitCodes = JavaUtils.nonNullImmutable(successfulExitCodes);
    this.extraCmdLineArgs = JavaUtils.nonNullImmutable(extraCmdLineArgs);
    this.runningSentinel = runningSentinel;
    this.loggingTag = loggingTag;
    this.loggingExtraFields = JavaUtils.nonNullImmutable(loggingExtraFields);
    this.sigKillProcessesAfterMillis = sigKillProcessesAfterMillis;
    this.maxTaskThreads = maxTaskThreads;
    this.preserveTaskSandboxAfterFinish = preserveTaskSandboxAfterFinish;
    this.maxOpenFiles = maxOpenFiles;
    this.skipLogrotateAndCompress = skipLogrotateAndCompress;
    this.s3ArtifactSignatures = s3ArtifactSignatures;
    this.logrotateFrequency = logrotateFrequency;
  }

  public ExecutorDataBuilder toBuilder() {
    return new ExecutorDataBuilder(cmd, embeddedArtifacts, externalArtifacts, s3Artifacts, successfulExitCodes, runningSentinel, user, extraCmdLineArgs, loggingTag,
        loggingExtraFields, sigKillProcessesAfterMillis, maxTaskThreads, preserveTaskSandboxAfterFinish, maxOpenFiles, skipLogrotateAndCompress, s3ArtifactSignatures, logrotateFrequency);
  }

  @ApiModelProperty(required=true, value="Command for the custom executor to run")
  public String getCmd() {
    return cmd;
  }

  @ApiModelProperty(required=false)
  public Optional<String> getLoggingTag() {
    return loggingTag;
  }

  @ApiModelProperty(required=false)
  public Map<String, String> getLoggingExtraFields() {
    return loggingExtraFields;
  }

  @ApiModelProperty(required=false, value="A list of the full content of any embedded artifacts")
  public List<EmbeddedArtifact> getEmbeddedArtifacts() {
    return embeddedArtifacts;
  }

  @ApiModelProperty(required=false, value="A list of external artifacts for the executor to download")
  public List<ExternalArtifact> getExternalArtifacts() {
    return externalArtifacts;
  }

  @ApiModelProperty(required=false, value="Allowable exit codes for the task to be considered FINISHED instead of FAILED")
  public List<Integer> getSuccessfulExitCodes() {
    return successfulExitCodes;
  }

  @ApiModelProperty(required=false, value="Extra arguments in addition to any provided in the cmd field")
  public List<String> getExtraCmdLineArgs() {
    return extraCmdLineArgs;
  }

  @ApiModelProperty(required=false)
  public Optional<String> getRunningSentinel() {
    return runningSentinel;
  }

  @ApiModelProperty(required=false, value="Run the task process as this user")
  public Optional<String> getUser() {
    return user;
  }

  @ApiModelProperty(required=false, value="Send a sigkill to a process if it has not shut down this many millis after being sent a term signal")
  public Optional<Long> getSigKillProcessesAfterMillis() {
    return sigKillProcessesAfterMillis;
  }

  @ApiModelProperty(required=false, value="List of s3 artifacts for the executor to download")
  public List<S3Artifact> getS3Artifacts() {
    return s3Artifacts;
  }

  @ApiModelProperty(required=false, value="Maximum number of threads a task is allowed to use")
  public Optional<Integer> getMaxTaskThreads() {
    return maxTaskThreads;
  }

  @ApiModelProperty(required=false, value="If true, do not delete files in the task sandbox after the task process has terminated")
  public Optional<Boolean> getPreserveTaskSandboxAfterFinish() {
    return preserveTaskSandboxAfterFinish;
  }

  @ApiModelProperty(required=false, value="Maximum number of open files the task process is allowed")
  public Optional<Integer> getMaxOpenFiles() {
    return maxOpenFiles;
  }

  @ApiModelProperty(required=false, value="If true, do not run logrotate or compress old log files")
  public Optional<Boolean> getSkipLogrotateAndCompress() {
    return skipLogrotateAndCompress;
  }

  @ApiModelProperty(required=false, value="A list of signatures use to verify downloaded s3artifacts")
  public Optional<List<S3ArtifactSignature>> getS3ArtifactSignatures() {
    return s3ArtifactSignatures;
  }

  @ApiModelProperty(required=false, value="Run logrotate this often. Can be HOURLY, DAILY, WEEKLY, MONTHLY")
  public Optional<SingularityExecutorLogrotateFrequency> getLogrotateFrequency() {
    return logrotateFrequency;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("cmd", cmd)
      .add("embeddedArtifacts", embeddedArtifacts)
      .add("externalArtifacts", externalArtifacts)
      .add("s3Artifacts", s3Artifacts)
      .add("successfulExitCodes", successfulExitCodes)
      .add("runningSentinel", runningSentinel)
      .add("user", user)
      .add("extraCmdLineArgs", extraCmdLineArgs)
      .add("loggingTag", loggingTag)
      .add("loggingExtraFields", loggingExtraFields)
      .add("sigKillProcessesAfterMillis", sigKillProcessesAfterMillis)
      .add("maxTaskThreads", maxTaskThreads)
      .add("preserveTaskSandboxAfterFinish", preserveTaskSandboxAfterFinish)
      .add("maxOpenFiles", maxOpenFiles)
      .add("skipLogrotateAndCompress", skipLogrotateAndCompress)
      .add("s3ArtifactSignatures", s3ArtifactSignatures)
      .add("logrotateFrequency", logrotateFrequency)
      .add("builder", toBuilder())
      .toString();
  }
}
