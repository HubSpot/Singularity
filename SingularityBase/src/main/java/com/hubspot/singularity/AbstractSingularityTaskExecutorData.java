package com.hubspot.singularity;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.google.common.base.Optional;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.deploy.ExecutorDataIF;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
public abstract class AbstractSingularityTaskExecutorData implements ExecutorDataIF {

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

  public abstract List<SingularityS3UploaderFile> getS3UploaderAdditionalFiles();

  public abstract String getDefaultS3Bucket();

  public abstract String getS3UploaderKeyPattern();

  public abstract String getServiceLog();

  public abstract String getServiceFinishedTailLog();

  public abstract Optional<String> getRequestGroup();

  public abstract Optional<String> getS3StorageClass();

  public abstract Optional<Long> getApplyS3StorageClassAfterBytes();
}
