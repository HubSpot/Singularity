package com.hubspot.singularity.api.task;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyleNoPublicConstructor;
import com.hubspot.singularity.api.deploy.EmbeddedArtifact;
import com.hubspot.singularity.api.deploy.ExecutorDataBase;
import com.hubspot.singularity.api.deploy.ExternalArtifact;
import com.hubspot.singularity.api.deploy.S3Artifact;
import com.hubspot.singularity.api.deploy.S3ArtifactSignature;
import com.hubspot.singularity.api.deploy.SingularityExecutorLogrotateFrequency;
import com.hubspot.singularity.api.logs.SingularityS3UploaderFile;

@Immutable
@SingularityStyleNoPublicConstructor
public abstract class AbstractSingularityTaskExecutorData implements ExecutorDataBase {
 /* public SingularityTaskExecutorData(ExecutorData executorData, List<SingularityS3UploaderFile> s3UploaderAdditionalFiles, String defaultS3Bucket, String s3UploaderKeyPattern,
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
  }*/

  public abstract String getCmd();
  public abstract Optional<String> getLoggingTag();
  public abstract Map<String, String> getLoggingExtraFields();
  public abstract List<EmbeddedArtifact> getEmbeddedArtifacts();
  public abstract List<ExternalArtifact> getExternalArtifacts();
  public abstract List<Integer> getSuccessfulExitCodes();
  public abstract List<String> getExtraCmdLineArgs();
  public abstract Optional<String> getRunningSentinel();
  public abstract Optional<String> getUser();
  public abstract Optional<Long> getSigKillProcessesAfterMillis();
  public abstract List<S3Artifact> getS3Artifacts();
  public abstract Optional<Integer> getMaxTaskThreads();
  public abstract Optional<Boolean> getPreserveTaskSandboxAfterFinish();
  public abstract Optional<Integer> getMaxOpenFiles();
  public abstract Optional<Boolean> getSkipLogrotateAndCompress();
  public abstract Optional<List<S3ArtifactSignature>> getS3ArtifactSignatures();
  public abstract Optional<SingularityExecutorLogrotateFrequency> getLogrotateFrequency();

  public abstract List<SingularityS3UploaderFile> getS3UploaderAdditionalFiles();

  public abstract String getDefaultS3Bucket();

  public abstract String getS3UploaderKeyPattern();

  public abstract String getServiceLog();

  public abstract String getServiceFinishedTailLog();

  public abstract Optional<String> getRequestGroup();

  public abstract Optional<String> getS3StorageClass();

  public abstract Optional<Long> getApplyS3StorageClassAfterBytes();
}
