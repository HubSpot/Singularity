package com.hubspot.singularity.s3downloader.config;

import java.util.concurrent.TimeUnit;

import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.runner.base.configuration.BaseRunnerConfiguration;
import com.hubspot.singularity.runner.base.configuration.Configuration;

@Configuration(filename = "/etc/singularity.s3downloader.yaml", consolidatedField = "s3downloader")
public class SingularityS3DownloaderConfiguration extends BaseRunnerConfiguration {
  @Min(1)
  @JsonProperty
  private long httpServerTimeout = TimeUnit.MINUTES.toMillis(30);

  @Min(1)
  @JsonProperty
  private int numEnqueueThreads = 10;

  @Min(1)
  @JsonProperty
  private long millisToWaitForReEnqueue = TimeUnit.SECONDS.toMillis(5);

  @Min(1)
  @JsonProperty
  private int numDownloaderThreads = 5;

  public SingularityS3DownloaderConfiguration() {
    super(Optional.of("singularity-s3downloader.log"));
  }

  public int getNumEnqueueThreads() {
    return numEnqueueThreads;
  }

  public void setNumEnqueueThreads(int numEnqueueThreads) {
    this.numEnqueueThreads = numEnqueueThreads;
  }

  public long getMillisToWaitForReEnqueue() {
    return millisToWaitForReEnqueue;
  }

  public void setMillisToWaitForReEnqueue(long millisToWaitForReEnqueue) {
    this.millisToWaitForReEnqueue = millisToWaitForReEnqueue;
  }

  public long getHttpServerTimeout() {
    return httpServerTimeout;
  }

  public void setHttpServerTimeout(long httpServerTimeout) {
    this.httpServerTimeout = httpServerTimeout;
  }

  public int getNumDownloaderThreads() {
    return numDownloaderThreads;
  }

  public void setNumDownloaderThreads(int numDownloaderThreads) {
    this.numDownloaderThreads = numDownloaderThreads;
  }

  @Override
  public String toString() {
    return "SingularityS3DownloaderConfiguration [httpServerTimeout=" + httpServerTimeout + ", numEnqueueThreads=" + numEnqueueThreads + ", millisToWaitForReEnqueue=" + millisToWaitForReEnqueue
        + ", numDownloaderThreads=" + numDownloaderThreads + "]";
  }
}
