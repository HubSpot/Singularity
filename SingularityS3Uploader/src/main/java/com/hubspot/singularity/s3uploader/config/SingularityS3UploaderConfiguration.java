package com.hubspot.singularity.s3uploader.config;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseConfigurationLoader;

@Singleton
public class SingularityS3UploaderConfiguration {

  private final long pollForShutDownMillis;
  private final int executorMaxUploadThreads;

  private final long checkUploadsEverySeconds;
  private final long stopCheckingAfterMillisWithoutNewFile;

  private final Path s3MetadataDirectory;
  private final String s3MetadataSuffix;

  @Inject
  public SingularityS3UploaderConfiguration(
      @Named(SingularityS3UploaderConfigurationLoader.POLL_MILLIS) String pollForShutDownMillis,
      @Named(SingularityS3UploaderConfigurationLoader.EXECUTOR_MAX_UPLOAD_THREADS) String executorMaxUploadThreads,
      @Named(SingularityRunnerBaseConfigurationLoader.S3_METADATA_DIRECTORY) String s3MetadataDirectory,
      @Named(SingularityRunnerBaseConfigurationLoader.S3_METADATA_SUFFIX) String s3MetadataSuffix,
      @Named(SingularityS3UploaderConfigurationLoader.CHECK_FOR_UPLOADS_EVERY_SECONDS) String checkUploadsEverySeconds,
      @Named(SingularityS3UploaderConfigurationLoader.STOP_CHECKING_AFTER_HOURS_WITHOUT_NEW_FILE) String stopCheckingAfterHoursWithoutNewFile
      ) {
    this.pollForShutDownMillis = Long.parseLong(pollForShutDownMillis);
    this.executorMaxUploadThreads = Integer.parseInt(executorMaxUploadThreads);
    this.s3MetadataDirectory = JavaUtils.getValidDirectory(s3MetadataDirectory, SingularityRunnerBaseConfigurationLoader.S3_METADATA_DIRECTORY);
    this.s3MetadataSuffix = s3MetadataSuffix;
    this.checkUploadsEverySeconds = Long.parseLong(checkUploadsEverySeconds);
    this.stopCheckingAfterMillisWithoutNewFile = TimeUnit.HOURS.toMillis(Long.parseLong(stopCheckingAfterHoursWithoutNewFile));
  }

  public Path getS3MetadataDirectory() {
    return s3MetadataDirectory;
  }

  public String getS3MetadataSuffix() {
    return s3MetadataSuffix;
  }

  public int getExecutorMaxUploadThreads() {
    return executorMaxUploadThreads;
  }

  public long getPollForShutDownMillis() {
    return pollForShutDownMillis;
  }

  public long getCheckUploadsEverySeconds() {
    return checkUploadsEverySeconds;
  }

  public long getStopCheckingAfterMillisWithoutNewFile() {
    return stopCheckingAfterMillisWithoutNewFile;
  }

  @Override
  public String toString() {
    return "SingularityS3UploaderConfiguration [pollForShutDownMillis=" + pollForShutDownMillis + ", executorMaxUploadThreads=" + executorMaxUploadThreads + ", checkUploadsEverySeconds=" + checkUploadsEverySeconds
        + ", stopCheckingAfterMillisWithoutNewFile=" + stopCheckingAfterMillisWithoutNewFile + ", s3MetadataDirectory=" + s3MetadataDirectory + ", s3MetadataSuffix=" + s3MetadataSuffix + "]";
  }

}
