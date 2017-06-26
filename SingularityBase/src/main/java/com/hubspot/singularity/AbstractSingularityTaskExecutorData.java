package com.hubspot.singularity;

import java.util.List;
import java.util.Map;

import org.immutables.value.Value.Immutable;

import com.google.common.base.Optional;
import com.hubspot.deploy.EmbeddedArtifact;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.deploy.ExecutorDataBase;
import com.hubspot.deploy.ExternalArtifact;
import com.hubspot.deploy.S3ArtifactBase;
import com.hubspot.deploy.S3ArtifactBaseSignature;
import com.hubspot.immutables.style.SingularityStyle;
import com.hubspot.singularity.executor.SingularityExecutorLogrotateFrequency;

@Immutable
@SingularityStyle
public abstract class AbstractSingularityTaskExecutorData implements ExecutorDataBase {

  public static SingularityTaskExecutorData of(ExecutorData executorData,
                                               List<SingularityS3UploaderFile> s3UploaderAdditionalFiles,
                                               String defaultS3Bucket,
                                               String s3UploaderKeyPattern,
                                               String serviceLog,
                                               String serviceFinishedTailLog,
                                               Optional<String> requestGroup,
                                               Optional<String> s3StorageClass,
                                               Optional<Long> applyS3StorageClassAfterBytes) {
    return SingularityTaskExecutorData.builder().from(executorData)
        .setS3UploaderAdditionalFiles(s3UploaderAdditionalFiles)
        .setDefaultS3Bucket(defaultS3Bucket)
        .setS3UploaderKeyPattern(s3UploaderKeyPattern)
        .setServiceLog(serviceLog)
        .setServiceFinishedTailLog(serviceFinishedTailLog)
        .setRequestGroup(requestGroup)
        .setS3StorageClass(s3StorageClass)
        .setApplyS3StorageClassAfterBytes(applyS3StorageClassAfterBytes)
        .build();
  }

  public abstract String getCmd();

  public abstract List<EmbeddedArtifact> getEmbeddedArtifacts();

  public abstract List<ExternalArtifact> getExternalArtifacts();

  public abstract List<S3ArtifactBase> getS3Artifacts();

  public abstract List<Integer> getSuccessfulExitCodes();

  public abstract Optional<String> getUser();

  public abstract Optional<String> getRunningSentinel();

  public abstract List<String> getExtraCmdLineArgs();

  public abstract Optional<String> getLoggingTag();

  public abstract Map<String, String> getLoggingExtraFields();

  public abstract Optional<Long> getSigKillProcessesAfterMillis();

  public abstract Optional<Integer> getMaxTaskThreads();

  public abstract Optional<Boolean> getPreserveTaskSandboxAfterFinish();

  public abstract Optional<Integer> getMaxOpenFiles();

  public abstract Optional<Boolean> getSkipLogrotateAndCompress();

  public abstract List<S3ArtifactBaseSignature> getS3ArtifactSignatures();

  public abstract List<S3ArtifactBaseSignature> getS3ArtifactSignaturesOrEmpty();

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
