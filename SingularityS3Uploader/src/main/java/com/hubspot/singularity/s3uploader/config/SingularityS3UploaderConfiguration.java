package com.hubspot.singularity.s3uploader.config;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class SingularityS3UploaderConfiguration {

  private final long pollForShutDownMillis;
  private final String s3AccessKey;
  private final String s3SecretKey;
  private final int executorCoreThreads;
  
  @Inject
  public SingularityS3UploaderConfiguration(@Named(SingularityS3UploaderConfigurationLoader.POLL_MILLIS) String pollForShutDownMillis, 
      @Named(SingularityS3UploaderConfigurationLoader.S3_ACCESS_KEY) String s3AccessKey, 
      @Named(SingularityS3UploaderConfigurationLoader.S3_SECRET_KEY) String s3SecretKey,
      @Named(SingularityS3UploaderConfigurationLoader.EXECUTOR_CORE_THREADS) String executorCoreThreads) {
    this.pollForShutDownMillis = Long.parseLong(pollForShutDownMillis);
    this.s3AccessKey = s3AccessKey;
    this.s3SecretKey = s3SecretKey;
    this.executorCoreThreads = Integer.parseInt(executorCoreThreads);
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

  @Override
  public String toString() {
    return "SingularityS3UploaderConfiguration [pollForShutDownMillis=" + pollForShutDownMillis + ", s3AccessKey=" + s3AccessKey + ", s3SecretKey=" + s3SecretKey + ", executorCoreThreads=" + executorCoreThreads + "]";
  }
  
}
