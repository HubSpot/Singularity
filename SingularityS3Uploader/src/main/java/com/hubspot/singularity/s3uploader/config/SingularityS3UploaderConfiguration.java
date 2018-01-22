package com.hubspot.singularity.s3uploader.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.runner.base.configuration.BaseRunnerConfiguration;
import com.hubspot.singularity.runner.base.configuration.Configuration;
import com.hubspot.singularity.runner.base.jackson.Obfuscate;
import com.hubspot.singularity.s3.base.config.SingularityS3Credentials;

@Configuration(filename = "/etc/singularity.s3uploader.yaml", consolidatedField = "s3uploader")
public class SingularityS3UploaderConfiguration extends BaseRunnerConfiguration {
  @Min(0)
  @JsonProperty
  private long pollForShutDownMillis = 1000;

  @Min(1)
  @JsonProperty
  private int executorMaxUploadThreads = 25;

  @Min(1)
  @JsonProperty
  private long checkUploadsEverySeconds = 600;

  @Min(1)
  @JsonProperty
  private long stopCheckingAfterMillisWithoutNewFile = TimeUnit.HOURS.toMillis(168);

  @JsonProperty
  @Obfuscate
  private Optional<String> s3AccessKey = Optional.absent();

  @JsonProperty
  @Obfuscate
  private Optional<String> s3SecretKey = Optional.absent();

  @Max(5368709120L)
  @Min(5242880L)
  @JsonProperty
  private long maxSingleUploadSizeBytes = 5368709120L;

  @Min(5242880L)
  @JsonProperty
  private long uploadPartSize = 20971520L;

  @Min(0)
  @JsonProperty
  private int retryWaitMs = 1000;

  @Min(0)
  @JsonProperty
  private int retryCount = 2;

  @JsonProperty
  private boolean checkForOpenFiles = true;

  @NotNull
  @JsonProperty
  private Map<String, SingularityS3Credentials> s3BucketCredentials = new HashMap<>();

  @NotNull
  @Valid
  @JsonProperty
  private List<SingularityS3UploaderContentHeaders> s3ContentHeaders = new ArrayList<>();

  public SingularityS3UploaderConfiguration() {
    super(Optional.of("singularity-s3uploader.log"));
  }

  public long getPollForShutDownMillis() {
    return pollForShutDownMillis;
  }

  public void setPollForShutDownMillis(long pollForShutDownMillis) {
    this.pollForShutDownMillis = pollForShutDownMillis;
  }

  public int getExecutorMaxUploadThreads() {
    return executorMaxUploadThreads;
  }

  public void setExecutorMaxUploadThreads(int executorMaxUploadThreads) {
    this.executorMaxUploadThreads = executorMaxUploadThreads;
  }

  public long getCheckUploadsEverySeconds() {
    return checkUploadsEverySeconds;
  }

  public void setCheckUploadsEverySeconds(long checkUploadsEverySeconds) {
    this.checkUploadsEverySeconds = checkUploadsEverySeconds;
  }

  public long getStopCheckingAfterMillisWithoutNewFile() {
    return stopCheckingAfterMillisWithoutNewFile;
  }

  public void setStopCheckingAfterMillisWithoutNewFile(long stopCheckingAfterMillisWithoutNewFile) {
    this.stopCheckingAfterMillisWithoutNewFile = stopCheckingAfterMillisWithoutNewFile;
  }

  public Optional<String> getS3AccessKey() {
    return s3AccessKey;
  }

  public void setS3AccessKey(Optional<String> s3AccessKey) {
    this.s3AccessKey = s3AccessKey;
  }

  public Optional<String> getS3SecretKey() {
    return s3SecretKey;
  }

  public void setS3SecretKey(Optional<String> s3SecretKey) {
    this.s3SecretKey = s3SecretKey;
  }

  public long getMaxSingleUploadSizeBytes() {
    return maxSingleUploadSizeBytes;
  }

  public void setMaxSingleUploadSizeBytes(long maxSingleUploadSizeBytes) {
    this.maxSingleUploadSizeBytes = maxSingleUploadSizeBytes;
  }

  public long getUploadPartSize() {
    return uploadPartSize;
  }

  public void setUploadPartSize(long uploadPartSize) {
    this.uploadPartSize = uploadPartSize;
  }

  public int getRetryCount() {
    return retryCount;
  }

  public void setRetryCount(int retryCount) {
    this.retryCount = retryCount;
  }

  public int getRetryWaitMs() {
    return retryWaitMs;
  }

  public void setRetryWaitMs(int retryWaitMs) {
    this.retryWaitMs = retryWaitMs;
  }

  public boolean isCheckForOpenFiles() {
    return checkForOpenFiles;
  }

  public void setCheckForOpenFiles(boolean checkForOpenFiles) {
    this.checkForOpenFiles = checkForOpenFiles;
  }

  public Map<String, SingularityS3Credentials> getS3BucketCredentials() {
    return s3BucketCredentials;
  }

  public void setS3BucketCredentials(Map<String, SingularityS3Credentials> s3BucketCredentials) {
    this.s3BucketCredentials = s3BucketCredentials;
  }

  public List<SingularityS3UploaderContentHeaders> getS3ContentHeaders() {
    return s3ContentHeaders;
  }

  public void setS3ContentHeaders(List<SingularityS3UploaderContentHeaders> s3ContentHeaders) {
    this.s3ContentHeaders = s3ContentHeaders;
  }

  @Override
  public String toString() {
    return "SingularityS3UploaderConfiguration{" +
        "pollForShutDownMillis=" + pollForShutDownMillis +
        ", executorMaxUploadThreads=" + executorMaxUploadThreads +
        ", checkUploadsEverySeconds=" + checkUploadsEverySeconds +
        ", stopCheckingAfterMillisWithoutNewFile=" + stopCheckingAfterMillisWithoutNewFile +
        ", s3AccessKey=" + s3AccessKey +
        ", s3SecretKey=" + s3SecretKey +
        ", maxSingleUploadSizeBytes=" + maxSingleUploadSizeBytes +
        ", uploadPartSize=" + uploadPartSize +
        ", retryWaitMs=" + retryWaitMs +
        ", retryCount=" + retryCount +
        ", checkForOpenFiles=" + checkForOpenFiles +
        ", s3BucketCredentials=" + s3BucketCredentials +
        ", s3ContentHeaders=" + s3ContentHeaders +
        "} " + super.toString();
  }
}
