package com.hubspot.singularity.s3downloader.config;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class SingularityS3DownloaderConfiguration {

  private final long httpServerTimeout;
  private final int numDownloaderThreads;

  @Inject
  public SingularityS3DownloaderConfiguration(
      @Named(SingularityS3DownloaderConfigurationLoader.HTTP_SERVER_TIMEOUT) String httpServerTimeout,
      @Named(SingularityS3DownloaderConfigurationLoader.NUM_DOWNLOADER_THREADS) String numDownloaderThreads
      ) {
    this.httpServerTimeout = Long.parseLong(httpServerTimeout);
    this.numDownloaderThreads = Integer.parseInt(numDownloaderThreads);
  }

  public int getNumDownloaderThreads() {
    return numDownloaderThreads;
  }

  public long getHttpServerTimeout() {
    return httpServerTimeout;
  }

  @Override
  public String toString() {
    return "SingularityS3DownloaderConfiguration [httpServerTimeout=" + httpServerTimeout + ", numDownloaderThreads=" + numDownloaderThreads + "]";
  }

}
