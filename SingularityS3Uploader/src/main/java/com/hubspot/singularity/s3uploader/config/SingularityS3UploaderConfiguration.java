package com.hubspot.singularity.s3uploader.config;

import java.util.Properties;

import javax.validation.constraints.Min;

import com.google.common.base.Optional;
import com.hubspot.singularity.runner.base.configuration.BaseRunnerConfiguration;
import com.hubspot.singularity.runner.base.configuration.Configuration;

@Configuration("/etc/singularity.s3uploader.yaml")
public class SingularityS3UploaderConfiguration extends BaseRunnerConfiguration {
  public static final String POLL_MILLIS = "s3uploader.poll.for.shutdown.millis";

  public static final String CHECK_FOR_UPLOADS_EVERY_SECONDS = "s3uploader.check.uploads.every.seconds";
  public static final String STOP_CHECKING_AFTER_HOURS_WITHOUT_NEW_FILE = "s3uploader.stop.checking.after.hours.without.new.file";

  public static final String EXECUTOR_MAX_UPLOAD_THREADS = "s3uploader.max.upload.threads";

  @Min(0)
  private long pollForShutDownMillis = 1000;

  @Min(1)
  private int executorMaxUploadThreads = 25;

  @Min(1)
  private long checkUploadsEverySeconds = 600;

  @Min(1)
  private long stopCheckingAfterMillisWithoutNewFile = 168;

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

  @Override
  public String toString() {
    return "SingularityS3UploaderConfiguration[" +
            "pollForShutDownMillis=" + pollForShutDownMillis +
            ", executorMaxUploadThreads=" + executorMaxUploadThreads +
            ", checkUploadsEverySeconds=" + checkUploadsEverySeconds +
            ", stopCheckingAfterMillisWithoutNewFile=" + stopCheckingAfterMillisWithoutNewFile +
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
  }
}
