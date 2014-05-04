package com.hubspot.singularity.s3uploader.config;

import java.nio.file.Path;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseConfigurationLoader;

public class SingularityS3UploaderConfiguration {

  private final long pollForShutDownMillis;
  private final String s3AccessKey;
  private final String s3SecretKey;
  private final int executorCoreThreads;

  private final long checkUploadsEverySeconds;
  private final long stopCheckingAfterSecondsWithoutNewFile;
  
  private final Path s3MetadataDirectory;
  private final String s3MetadataSuffix;
  
  @Inject
  public SingularityS3UploaderConfiguration(@Named(SingularityS3UploaderConfigurationLoader.POLL_MILLIS) String pollForShutDownMillis, 
      @Named(SingularityS3UploaderConfigurationLoader.S3_ACCESS_KEY) String s3AccessKey, 
      @Named(SingularityS3UploaderConfigurationLoader.S3_SECRET_KEY) String s3SecretKey,
      @Named(SingularityS3UploaderConfigurationLoader.EXECUTOR_CORE_THREADS) String executorCoreThreads,
      @Named(SingularityS3UploaderConfigurationLoader.S3_METADATA_DIRECTORY) String s3MetadataDirectory,
      @Named(SingularityS3UploaderConfigurationLoader.S3_METADATA_SUFFIX) String s3MetadataSuffix,
      @Named(SingularityS3UploaderConfigurationLoader.CHECK_FOR_UPLOADS_EVERY_SECONDS) String checkUploadsEverySeconds,
      @Named(SingularityS3UploaderConfigurationLoader.STOP_CHECKING_AFTER_SECONDS_WITHOUT_NEW_FILE) String stopCheckingAfterSecondsWithoutNewFile
      ) {
    this.pollForShutDownMillis = Long.parseLong(pollForShutDownMillis);
    this.s3AccessKey = s3AccessKey;
    this.s3SecretKey = s3SecretKey;
    this.executorCoreThreads = Integer.parseInt(executorCoreThreads);
    this.s3MetadataDirectory = SingularityRunnerBaseConfigurationLoader.getValidDirectory(s3MetadataDirectory, SingularityS3UploaderConfigurationLoader.S3_METADATA_DIRECTORY);
    this.s3MetadataSuffix = s3MetadataSuffix;
    this.checkUploadsEverySeconds = Long.parseLong(checkUploadsEverySeconds);
    this.stopCheckingAfterSecondsWithoutNewFile = Long.parseLong(stopCheckingAfterSecondsWithoutNewFile);
  }

  public Path getS3MetadataDirectory() {
    return s3MetadataDirectory;
  }

  public String getS3MetadataSuffix() {
    return s3MetadataSuffix;
  }

  public int getExecutorCoreThreads() {
    return executorCoreThreads;
  }

  public long getPollForShutDownMillis() {
    return pollForShutDownMillis;
  }

  public String getS3AccessKey() {
    return s3AccessKey;
  }

  public String getS3SecretKey() {
    return s3SecretKey;
  }

  public long getCheckUploadsEverySeconds() {
    return checkUploadsEverySeconds;
  }

  public long getStopCheckingAfterSecondsWithoutNewFile() {
    return stopCheckingAfterSecondsWithoutNewFile;
  }

  @Override
  public String toString() {
    return "SingularityS3UploaderConfiguration [pollForShutDownMillis=" + pollForShutDownMillis + ", s3AccessKey=" + s3AccessKey + ", s3SecretKey=" + s3SecretKey + ", executorCoreThreads=" + executorCoreThreads
        + ", checkUploadsEverySeconds=" + checkUploadsEverySeconds + ", stopCheckingAfterSecondsWithoutNewFile=" + stopCheckingAfterSecondsWithoutNewFile + ", s3MetadataDirectory=" + s3MetadataDirectory + ", s3MetadataSuffix="
        + s3MetadataSuffix + "]";
  }
  
}
