package com.hubspot.deploy;

import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;
import com.hubspot.singularity.executor.SingularityExecutorLogrotateFrequency;

public class ExecutorDataBuilder {

  private String cmd;
  private List<EmbeddedArtifact> embeddedArtifacts;
  private List<ExternalArtifact> externalArtifacts;
  private List<S3Artifact> s3Artifacts;
  private List<Integer> successfulExitCodes;
  private Optional<String> runningSentinel;
  private Optional<String> user;
  private List<String> extraCmdLineArgs;
  private Optional<String> loggingTag;
  private Map<String, String> loggingExtraFields;
  private Optional<Long> sigKillProcessesAfterMillis;
  private Optional<Integer> maxTaskThreads;
  private Optional<Boolean> preserveTaskSandboxAfterFinish;
  private Optional<Integer> maxOpenFiles;
  private Optional<Boolean> skipLogrotateAndCompress;
  private Optional<List<S3ArtifactSignature>> s3ArtifactSignatures;
  private Optional<SingularityExecutorLogrotateFrequency> logrotateFrequency;

  public ExecutorDataBuilder(String cmd, List<EmbeddedArtifact> embeddedArtifacts, List<ExternalArtifact> externalArtifacts, List<S3Artifact> s3Artifacts, List<Integer> successfulExitCodes,
      Optional<String> runningSentinel, Optional<String> user, List<String> extraCmdLineArgs, Optional<String> loggingTag, Map<String, String> loggingExtraFields,
      Optional<Long> sigKillProcessesAfterMillis, Optional<Integer> maxTaskThreads, Optional<Boolean> preserveTaskSandboxAfterFinish,
      Optional<Integer> maxOpenFiles, Optional<Boolean> skipLogrotateAndCompress, Optional<List<S3ArtifactSignature>> s3ArtifactSignatures, Optional<SingularityExecutorLogrotateFrequency> logrotateFrequency) {
    this.cmd = cmd;
    this.embeddedArtifacts = embeddedArtifacts;
    this.externalArtifacts = externalArtifacts;
    this.s3Artifacts = s3Artifacts;
    this.successfulExitCodes = successfulExitCodes;
    this.runningSentinel = runningSentinel;
    this.user = user;
    this.extraCmdLineArgs = extraCmdLineArgs;
    this.loggingTag = loggingTag;
    this.loggingExtraFields = loggingExtraFields;
    this.sigKillProcessesAfterMillis = sigKillProcessesAfterMillis;
    this.maxTaskThreads = maxTaskThreads;
    this.preserveTaskSandboxAfterFinish = preserveTaskSandboxAfterFinish;
    this.maxOpenFiles = maxOpenFiles;
    this.skipLogrotateAndCompress = skipLogrotateAndCompress;
    this.s3ArtifactSignatures = s3ArtifactSignatures;
    this.logrotateFrequency = logrotateFrequency;
  }

  public ExecutorDataBuilder() {

  }

  public ExecutorData build() {
    return new ExecutorData(cmd, embeddedArtifacts, externalArtifacts, s3Artifacts, successfulExitCodes, user, runningSentinel, extraCmdLineArgs, loggingTag, loggingExtraFields,
        sigKillProcessesAfterMillis, maxTaskThreads, preserveTaskSandboxAfterFinish, maxOpenFiles, skipLogrotateAndCompress, s3ArtifactSignatures, logrotateFrequency);
  }

  public Optional<String> getLoggingTag() {
    return loggingTag;
  }

  public ExecutorDataBuilder setLoggingTag(Optional<String> loggingTag) {
    this.loggingTag = loggingTag;
    return this;
  }

  public Map<String, String> getLoggingExtraFields() {
    return loggingExtraFields;
  }

  public ExecutorDataBuilder setLoggingExtraFields(Map<String, String> loggingExtraFields) {
    this.loggingExtraFields = loggingExtraFields;
    return this;
  }

  public String getCmd() {
    return cmd;
  }

  public List<EmbeddedArtifact> getEmbeddedArtifacts() {
    return embeddedArtifacts;
  }

  public List<ExternalArtifact> getExternalArtifacts() {
    return externalArtifacts;
  }

  public List<Integer> getSuccessfulExitCodes() {
    return successfulExitCodes;
  }

  public List<String> getExtraCmdLineArgs() {
    return extraCmdLineArgs;
  }

  public Optional<String> getRunningSentinel() {
    return runningSentinel;
  }

  public Optional<String> getUser() {
    return user;
  }

  public ExecutorDataBuilder setCmd(String cmd) {
    this.cmd = cmd;
    return this;
  }

  public Optional<Long> getSigKillProcessesAfterMillis() {
    return sigKillProcessesAfterMillis;
  }

  public ExecutorDataBuilder setSigKillProcessesAfterMillis(Optional<Long> sigKillProcessesAfterMillis) {
    this.sigKillProcessesAfterMillis = sigKillProcessesAfterMillis;
    return this;
  }

  public ExecutorDataBuilder setEmbeddedArtifacts(List<EmbeddedArtifact> embeddedArtifacts) {
    this.embeddedArtifacts = embeddedArtifacts;
    return this;
  }

  public ExecutorDataBuilder setExternalArtifacts(List<ExternalArtifact> externalArtifacts) {
    this.externalArtifacts = externalArtifacts;
    return this;
  }

  public ExecutorDataBuilder setSuccessfulExitCodes(List<Integer> successfulExitCodes) {
    this.successfulExitCodes = successfulExitCodes;
    return this;
  }

  public ExecutorDataBuilder setRunningSentinel(Optional<String> runningSentinel) {
    this.runningSentinel = runningSentinel;
    return this;
  }

  public ExecutorDataBuilder setUser(Optional<String> user) {
    this.user = user;
    return this;
  }

  public ExecutorDataBuilder setExtraCmdLineArgs(List<String> extraCmdLineArgs) {
    this.extraCmdLineArgs = extraCmdLineArgs;
    return this;
  }

  public List<S3Artifact> getS3Artifacts() {
    return s3Artifacts;
  }

  public ExecutorDataBuilder setS3Artifacts(List<S3Artifact> s3Artifacts) {
    this.s3Artifacts = s3Artifacts;
    return this;
  }

  public Optional<Integer> getMaxTaskThreads() {
    return maxTaskThreads;
  }

  public ExecutorDataBuilder setMaxTaskThreads(Optional<Integer> maxTaskThreads) {
    this.maxTaskThreads = maxTaskThreads;
    return this;
  }

  public Optional<Boolean> getPreserveTaskSandboxAfterFinish() {
    return preserveTaskSandboxAfterFinish;
  }

  public ExecutorDataBuilder setPreserveTaskSandboxAfterFinish(Optional<Boolean> preserveTaskSandboxAfterFinish) {
    this.preserveTaskSandboxAfterFinish = preserveTaskSandboxAfterFinish;
    return this;
  }

  public Optional<Integer> getMaxOpenFiles() {
    return maxOpenFiles;
  }

  public ExecutorDataBuilder setMaxOpenFiles(Optional<Integer> maxOpenFiles) {
    this.maxOpenFiles = maxOpenFiles;
    return this;
  }

  public Optional<Boolean> getSkipLogrotateAndCompress() {
    return skipLogrotateAndCompress;
  }

  public ExecutorDataBuilder setSkipLogrotateAndCompress(Optional<Boolean> skipLogrotateAndCompress) {
    this.skipLogrotateAndCompress = skipLogrotateAndCompress;
    return this;
  }

  public Optional<List<S3ArtifactSignature>> getS3ArtifactSignatures() {
    return s3ArtifactSignatures;
  }

  public ExecutorDataBuilder setS3ArtifactSignatures(Optional<List<S3ArtifactSignature>> s3ArtifactSignatures) {
    this.s3ArtifactSignatures = s3ArtifactSignatures;
    return this;
  }

  public Optional<SingularityExecutorLogrotateFrequency> getLogrotateFrequency() {
    return logrotateFrequency;
  }

  public ExecutorDataBuilder setLogrotateFrequency(Optional<SingularityExecutorLogrotateFrequency> logrotateFrequency) {
    this.logrotateFrequency = logrotateFrequency;
    return this;
  }

  @Override
  public String toString() {
    return "ExecutorDataBuilder{" +
        "cmd='" + cmd + '\'' +
        ", embeddedArtifacts=" + embeddedArtifacts +
        ", externalArtifacts=" + externalArtifacts +
        ", s3Artifacts=" + s3Artifacts +
        ", successfulExitCodes=" + successfulExitCodes +
        ", runningSentinel=" + runningSentinel +
        ", user=" + user +
        ", extraCmdLineArgs=" + extraCmdLineArgs +
        ", loggingTag=" + loggingTag +
        ", loggingExtraFields=" + loggingExtraFields +
        ", sigKillProcessesAfterMillis=" + sigKillProcessesAfterMillis +
        ", maxTaskThreads=" + maxTaskThreads +
        ", preserveTaskSandboxAfterFinish=" + preserveTaskSandboxAfterFinish +
        ", maxOpenFiles=" + maxOpenFiles +
        ", skipLogrotateAndCompress=" + skipLogrotateAndCompress +
        ", s3ArtifactSignatures=" + s3ArtifactSignatures +
        ", logrotateFrequency=" + logrotateFrequency +
        '}';
  }
}
