package com.hubspot.deploy;

import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;

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
  private Optional<String> loggingS3Bucket;

  public ExecutorDataBuilder(String cmd, List<EmbeddedArtifact> embeddedArtifacts, List<ExternalArtifact> externalArtifacts, List<S3Artifact> s3Artifacts, List<Integer> successfulExitCodes, Optional<String> runningSentinel,
      Optional<String> user, List<String> extraCmdLineArgs, Optional<String> loggingTag, Map<String, String> loggingExtraFields, Optional<Long> sigKillProcessesAfterMillis, Optional<Integer> maxTaskThreads, Optional<String> loggingS3Bucket) {
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
    this.loggingS3Bucket = loggingS3Bucket;
  }

  public ExecutorDataBuilder() {

  }

  public ExecutorData build() {
    return new ExecutorData(cmd, embeddedArtifacts, externalArtifacts, s3Artifacts, successfulExitCodes, user, runningSentinel, extraCmdLineArgs, loggingTag, loggingExtraFields, sigKillProcessesAfterMillis, maxTaskThreads, loggingS3Bucket);
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

  public Optional<String> getLoggingS3Bucket() {
    return loggingS3Bucket;
  }

  public ExecutorDataBuilder setLoggingS3Bucket(Optional<String> loggingS3Bucket) {
    this.loggingS3Bucket = loggingS3Bucket;
    return this;
  }

  @Override
  public String toString() {
    return "ExecutorDataBuilder[" +
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
            ", loggingS3Bucket=" + loggingS3Bucket +
            ']';
  }
}
