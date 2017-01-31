package com.hubspot.singularity;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.deploy.EmbeddedArtifact;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.deploy.ExternalArtifact;
import com.hubspot.deploy.S3Artifact;
import com.hubspot.deploy.S3ArtifactSignature;
import com.hubspot.singularity.executor.SingularityExecutorLogrotateFrequency;

public class SingularityTaskExecutorData extends ExecutorData {
  private final List<SingularityS3UploaderFile> s3UploaderAdditionalFiles;
  private final String defaultS3Bucket;
  private final String s3UploaderKeyPattern;
  private final String serviceLog;
  private final String serviceFinishedTailLog;
  private final Optional<String> requestGroup;
  private final Optional<String> s3StorageClass;
  private final Optional<Long> applyS3StorageClassAfterBytes;

  public SingularityTaskExecutorData(ExecutorData executorData, List<SingularityS3UploaderFile> s3UploaderAdditionalFiles, String defaultS3Bucket, String s3UploaderKeyPattern,
       String serviceLog, String serviceFinishedTailLog, Optional<String> requestGroup, Optional<String> s3StorageClass, Optional<Long> applyS3StorageClassAfterBytes) {
    this(executorData.getCmd(),
        executorData.getEmbeddedArtifacts(),
        executorData.getExternalArtifacts(),
        executorData.getS3Artifacts(),
        executorData.getSuccessfulExitCodes(),
        executorData.getUser(),
        executorData.getRunningSentinel(),
        executorData.getExtraCmdLineArgs(),
        executorData.getLoggingTag(),
        executorData.getLoggingExtraFields(),
        executorData.getSigKillProcessesAfterMillis(),
        executorData.getMaxTaskThreads(),
        executorData.getPreserveTaskSandboxAfterFinish(),
        executorData.getMaxOpenFiles(),
        executorData.getSkipLogrotateAndCompress(),
        executorData.getS3ArtifactSignatures(),
        executorData.getLogrotateFrequency(),
        s3UploaderAdditionalFiles,
        defaultS3Bucket,
        s3UploaderKeyPattern,
        serviceLog,
        serviceFinishedTailLog,
        requestGroup,
        s3StorageClass,
        applyS3StorageClassAfterBytes);
  }

  @JsonCreator
  public SingularityTaskExecutorData(@JsonProperty("cmd") String cmd,
                                     @JsonProperty("embeddedArtifacts") List<EmbeddedArtifact> embeddedArtifacts,
                                     @JsonProperty("externalArtifacts") List<ExternalArtifact> externalArtifacts,
                                     @JsonProperty("s3Artifacts") List<S3Artifact> s3Artifacts,
                                     @JsonProperty("successfulExitCodes") List<Integer> successfulExitCodes,
                                     @JsonProperty("user") Optional<String> user,
                                     @JsonProperty("runningSentinel") Optional<String> runningSentinel,
                                     @JsonProperty("extraCmdLineArgs") List<String> extraCmdLineArgs,
                                     @JsonProperty("loggingTag") Optional<String> loggingTag,
                                     @JsonProperty("loggingExtraFields") Map<String, String> loggingExtraFields,
                                     @JsonProperty("sigKillProcessesAfterMillis") Optional<Long> sigKillProcessesAfterMillis,
                                     @JsonProperty("maxTaskThreads") Optional<Integer> maxTaskThreads,
                                     @JsonProperty("preserveTaskSandboxAfterFinish") Optional<Boolean> preserveTaskSandboxAfterFinish,
                                     @JsonProperty("maxOpenFiles") Optional<Integer> maxOpenFiles,
                                     @JsonProperty("skipLogrotateAndCompress") Optional<Boolean> skipLogrotateAndCompress,
                                     @JsonProperty("s3ArtifactSignatures") Optional<List<S3ArtifactSignature>> s3ArtifactSignatures,
                                     @JsonProperty("logrotateFrequency") Optional<SingularityExecutorLogrotateFrequency> logrotateFrequency,
                                     @JsonProperty("s3UploaderAdditionalFiles") List<SingularityS3UploaderFile> s3UploaderAdditionalFiles,
                                     @JsonProperty("defaultS3Bucket") String defaultS3Bucket,
                                     @JsonProperty("s3UploaderKeyPattern") String s3UploaderKeyPattern,
                                     @JsonProperty("serviceLog") String serviceLog,
                                     @JsonProperty("serviceFinishedTailLog") String serviceFinishedTailLog,
                                     @JsonProperty("requestGroup")  Optional<String> requestGroup,
                                     @JsonProperty("s3StorageClass") Optional<String> s3StorageClass,
                                     @JsonProperty("applyS3StorageClassAfterBytes") Optional<Long> applyS3StorageClassAfterBytes) {
    super(cmd, embeddedArtifacts, externalArtifacts, s3Artifacts, successfulExitCodes, user, runningSentinel, extraCmdLineArgs, loggingTag, loggingExtraFields,
        sigKillProcessesAfterMillis, maxTaskThreads, preserveTaskSandboxAfterFinish, maxOpenFiles, skipLogrotateAndCompress, s3ArtifactSignatures, logrotateFrequency);
    this.s3UploaderAdditionalFiles = s3UploaderAdditionalFiles;
    this.defaultS3Bucket = defaultS3Bucket;
    this.s3UploaderKeyPattern = s3UploaderKeyPattern;
    this.serviceLog = serviceLog;
    this.serviceFinishedTailLog = serviceFinishedTailLog;
    this.requestGroup = requestGroup;
    this.s3StorageClass = s3StorageClass;
    this.applyS3StorageClassAfterBytes = applyS3StorageClassAfterBytes;
  }

  public List<SingularityS3UploaderFile> getS3UploaderAdditionalFiles() {
    return s3UploaderAdditionalFiles;
  }

  public String getDefaultS3Bucket() {
    return defaultS3Bucket;
  }

  public String getS3UploaderKeyPattern() {
    return s3UploaderKeyPattern;
  }

  public String getServiceLog() {
    return serviceLog;
  }

  public String getServiceFinishedTailLog() {
    return serviceFinishedTailLog;
  }

  public Optional<String> getRequestGroup() {
    return requestGroup;
  }

  public Optional<String> getS3StorageClass() {
    return s3StorageClass;
  }

  public Optional<Long> getApplyS3StorageClassAfterBytes() {
    return applyS3StorageClassAfterBytes;
  }

  @Override
  public String toString() {
    return "SingularityTaskExecutorData{" +
        "s3UploaderAdditionalFiles=" + s3UploaderAdditionalFiles +
        ", defaultS3Bucket='" + defaultS3Bucket + '\'' +
        ", s3UploaderKeyPattern='" + s3UploaderKeyPattern + '\'' +
        ", serviceLog='" + serviceLog + '\'' +
        ", serviceFinishedTailLog='" + serviceFinishedTailLog + '\'' +
        ", requestGroup=" + requestGroup +
        ", s3StorageClass=" + s3StorageClass +
        ", applyS3StorageClassAfterBytes=" + applyS3StorageClassAfterBytes +
        "} " + super.toString();
  }
}
