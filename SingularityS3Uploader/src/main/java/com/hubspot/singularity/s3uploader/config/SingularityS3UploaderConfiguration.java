package com.hubspot.singularity.s3uploader.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.runner.base.configuration.BaseRunnerConfiguration;
import com.hubspot.singularity.runner.base.configuration.Configuration;
import com.hubspot.singularity.runner.base.jackson.Obfuscate;
import com.hubspot.singularity.s3.base.config.SingularityS3Configuration;
import com.hubspot.singularity.s3.base.config.SingularityS3Credentials;

@Configuration(filename = "/etc/singularity.s3uploader.yaml", consolidatedField = "s3uploader")
public class SingularityS3UploaderConfiguration extends BaseRunnerConfiguration {
  public static final String POLL_MILLIS = "s3uploader.poll.for.shutdown.millis";

  public static final String CHECK_FOR_UPLOADS_EVERY_SECONDS = "s3uploader.check.uploads.every.seconds";
  public static final String STOP_CHECKING_AFTER_HOURS_WITHOUT_NEW_FILE = "s3uploader.stop.checking.after.hours.without.new.file";

  public static final String EXECUTOR_MAX_UPLOAD_THREADS = "s3uploader.max.upload.threads";

  public static final String MAX_SINGLE_UPLOAD_BYTES = "s3uploader.max.single.upload.size";
  public static final String UPLOAD_PART_SIZE = "s3uploader.upload.part.size";
  public static final String RETRY_WAIT_MS = "s3uploader.retry.wait.ms";
  public static final String RETRY_COUNT = "s3uploader.retry.count";

  public static final String CHECK_FOR_OPEN_FILES = "s3uploader.check.for.open.files";

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

  @NotNull
  @JsonProperty
  @Obfuscate
  private Optional<String> s3AccessKey = Optional.absent();

  @NotNull
  @JsonProperty
  @Obfuscate
  private Optional<String> s3SecretKey = Optional.absent();

  @JsonProperty
  private long maxSingleUploadSizeBytes = 5368709120L;

  @JsonProperty
  private long uploadPartSize = 20971520L;

  @JsonProperty
  private int retryWaitMs = 1000;

  @JsonProperty
  private int retryCount = 2;

  @JsonProperty
  private boolean checkForOpenFiles = true;

  @NotNull
  @JsonProperty
  private Map<String, SingularityS3Credentials> s3BucketCredentials = new HashMap<>();

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

  @Override
  public String toString() {
    return "SingularityS3UploaderConfiguration[" +
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
            ']';
  }

  @Override
  public void updateFromProperties(Properties properties) {
    if (properties.containsKey(POLL_MILLIS)) {
      setPollForShutDownMillis(Long.parseLong(properties.getProperty(POLL_MILLIS)));
    }

    if (properties.containsKey(CHECK_FOR_UPLOADS_EVERY_SECONDS)) {
      setCheckUploadsEverySeconds(Long.parseLong(properties.getProperty(CHECK_FOR_UPLOADS_EVERY_SECONDS)));
    }

    if (properties.containsKey(STOP_CHECKING_AFTER_HOURS_WITHOUT_NEW_FILE)) {
      setStopCheckingAfterMillisWithoutNewFile(Long.parseLong(properties.getProperty(STOP_CHECKING_AFTER_HOURS_WITHOUT_NEW_FILE)));
    }

    if (properties.containsKey(EXECUTOR_MAX_UPLOAD_THREADS)) {
      setExecutorMaxUploadThreads(Integer.parseInt(properties.getProperty(EXECUTOR_MAX_UPLOAD_THREADS)));
    }

    if (properties.containsKey(SingularityS3Configuration.S3_ACCESS_KEY)) {
      setS3AccessKey(Optional.of(properties.getProperty(SingularityS3Configuration.S3_ACCESS_KEY)));
    }

    if (properties.containsKey(SingularityS3Configuration.S3_SECRET_KEY)) {
      setS3SecretKey(Optional.of(properties.getProperty(SingularityS3Configuration.S3_SECRET_KEY)));
    }
    if (properties.containsKey(MAX_SINGLE_UPLOAD_BYTES)) {
      setMaxSingleUploadSizeBytes(Long.parseLong(properties.getProperty(MAX_SINGLE_UPLOAD_BYTES)));
    }
    if (properties.containsKey(UPLOAD_PART_SIZE)) {
      setUploadPartSize(Long.parseLong(properties.getProperty(UPLOAD_PART_SIZE)));
    }
    if (properties.containsKey(RETRY_COUNT)) {
      setRetryCount(Integer.parseInt(properties.getProperty(RETRY_COUNT)));
    }
    if (properties.containsKey(RETRY_WAIT_MS)) {
      setRetryWaitMs(Integer.parseInt(RETRY_WAIT_MS));
    }
    if (properties.containsKey(CHECK_FOR_OPEN_FILES)) {
      setCheckForOpenFiles(Boolean.parseBoolean(properties.getProperty(CHECK_FOR_OPEN_FILES)));
    }
  }
}
