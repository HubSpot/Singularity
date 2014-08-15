package com.hubspot.singularity.s3.base.config;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class SingularityS3Configuration {

  private final String cacheDirectory;

  private final String s3AccessKey;
  private final String s3SecretKey;

  private final long s3ChunkSize;
  private final long s3DownloadTimeoutMillis;

  private final int localDownloadHttpPort;
  private final String localDownloadPath;

  @Inject
  public SingularityS3Configuration(
      @Named(SingularityS3ConfigurationLoader.ARTIFACT_CACHE_DIRECTORY) String cacheDirectory,
      @Named(SingularityS3ConfigurationLoader.S3_ACCESS_KEY) String s3AccessKey,
      @Named(SingularityS3ConfigurationLoader.S3_SECRET_KEY) String s3SecretKey,
      @Named(SingularityS3ConfigurationLoader.S3_CHUNK_SIZE) String s3ChunkSize,
      @Named(SingularityS3ConfigurationLoader.S3_DOWNLOAD_TIMEOUT_MILLIS) String s3DownloadTimeoutMillis,
      @Named(SingularityS3ConfigurationLoader.LOCAL_DOWNLOAD_HTTP_PORT) String localDownloadHttpPort,
      @Named(SingularityS3ConfigurationLoader.LOCAL_DOWNLOAD_HTTP_DOWNLOAD_PATH) String localDownloadPath

      ) {
    this.cacheDirectory = cacheDirectory;
    this.s3AccessKey = s3AccessKey;
    this.s3SecretKey = s3SecretKey;
    this.s3ChunkSize = Long.parseLong(s3ChunkSize);
    this.s3DownloadTimeoutMillis = Long.parseLong(s3DownloadTimeoutMillis);
    this.localDownloadHttpPort = Integer.parseInt(localDownloadHttpPort);
    this.localDownloadPath = localDownloadPath;
  }

  public int getLocalDownloadHttpPort() {
    return localDownloadHttpPort;
  }

  public String getLocalDownloadPath() {
    return localDownloadPath;
  }

  public String getCacheDirectory() {
    return cacheDirectory;
  }

  public String getS3AccessKey() {
    return s3AccessKey;
  }

  public String getS3SecretKey() {
    return s3SecretKey;
  }

  public long getS3ChunkSize() {
    return s3ChunkSize;
  }

  public long getS3DownloadTimeoutMillis() {
    return s3DownloadTimeoutMillis;
  }

  @Override
  public String toString() {
    return "SingularityS3Configuration [cacheDirectory=" + cacheDirectory + ", s3AccessKey=" + s3AccessKey + ", s3SecretKey=" + s3SecretKey + ", s3ChunkSize=" + s3ChunkSize + ", s3DownloadTimeoutMillis=" + s3DownloadTimeoutMillis
        + ", localDownloadHttpPort=" + localDownloadHttpPort + ", localDownloadPath=" + localDownloadPath + "]";
  }

}
