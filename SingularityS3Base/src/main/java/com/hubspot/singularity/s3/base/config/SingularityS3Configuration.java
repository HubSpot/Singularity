package com.hubspot.singularity.s3.base.config;

import org.hibernate.validator.constraints.NotEmpty;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.google.common.base.Optional;
import com.hubspot.singularity.runner.base.configuration.BaseRunnerConfiguration;
import com.hubspot.singularity.runner.base.configuration.Configuration;
import com.hubspot.singularity.runner.base.constraints.DirectoryExists;
import com.hubspot.singularity.runner.base.jackson.Obfuscate;

@Configuration("/etc/singularity.s3base.yaml")
public class SingularityS3Configuration extends BaseRunnerConfiguration {
  public static final String ARTIFACT_CACHE_DIRECTORY = "artifact.cache.directory";

  public static final String S3_ACCESS_KEY = "s3.access.key";
  public static final String S3_SECRET_KEY = "s3.secret.key";

  public static final String S3_CHUNK_SIZE = "s3.downloader.chunk.size";
  public static final String S3_DOWNLOAD_TIMEOUT_MILLIS = "s3.downloader.timeout.millis";

  public static final String LOCAL_DOWNLOAD_HTTP_PORT = "s3.downloader.http.port";
  public static final String LOCAL_DOWNLOAD_HTTP_DOWNLOAD_PATH = "s3.downloader.http.download.path";

  @NotEmpty
  @DirectoryExists
  private String cacheDirectory;

  @NotNull
  @Obfuscate
  private String s3AccessKey = "";

  @NotNull
  @Obfuscate
  private String s3SecretKey = "";

  @Min(1)
  private long s3ChunkSize = 104857600;

  @Min(1)
  private long s3DownloadTimeoutMillis = TimeUnit.MINUTES.toMillis(1);

  @Min(0)
  private int localDownloadHttpPort = 7070;

  @NotEmpty
  private String localDownloadPath = "/download";

  public SingularityS3Configuration() {
    super(Optional.<String>absent());
  }

  public String getCacheDirectory() {
    return cacheDirectory;
  }

  public void setCacheDirectory(String cacheDirectory) {
    this.cacheDirectory = cacheDirectory;
  }

  public String getS3AccessKey() {
    return s3AccessKey;
  }

  public void setS3AccessKey(String s3AccessKey) {
    this.s3AccessKey = s3AccessKey;
  }

  public String getS3SecretKey() {
    return s3SecretKey;
  }

  public void setS3SecretKey(String s3SecretKey) {
    this.s3SecretKey = s3SecretKey;
  }

  public long getS3ChunkSize() {
    return s3ChunkSize;
  }

  public void setS3ChunkSize(long s3ChunkSize) {
    this.s3ChunkSize = s3ChunkSize;
  }

  public long getS3DownloadTimeoutMillis() {
    return s3DownloadTimeoutMillis;
  }

  public void setS3DownloadTimeoutMillis(long s3DownloadTimeoutMillis) {
    this.s3DownloadTimeoutMillis = s3DownloadTimeoutMillis;
  }

  public int getLocalDownloadHttpPort() {
    return localDownloadHttpPort;
  }

  public void setLocalDownloadHttpPort(int localDownloadHttpPort) {
    this.localDownloadHttpPort = localDownloadHttpPort;
  }

  public String getLocalDownloadPath() {
    return localDownloadPath;
  }

  public void setLocalDownloadPath(String localDownloadPath) {
    this.localDownloadPath = localDownloadPath;
  }

  @Override
  public void updateFromProperties(Properties properties) {
    if (properties.containsKey(ARTIFACT_CACHE_DIRECTORY)) {
      setCacheDirectory(properties.getProperty(ARTIFACT_CACHE_DIRECTORY));
    }

    if (properties.containsKey(S3_ACCESS_KEY)) {
      setS3AccessKey(properties.getProperty(S3_ACCESS_KEY));
    }

    if (properties.containsKey(S3_SECRET_KEY)) {
      setS3SecretKey(properties.getProperty(S3_SECRET_KEY));
    }

    if (properties.containsKey(S3_CHUNK_SIZE)) {
      setS3ChunkSize(Long.parseLong(properties.getProperty(S3_CHUNK_SIZE)));
    }

    if (properties.containsKey(S3_DOWNLOAD_TIMEOUT_MILLIS)) {
      setS3DownloadTimeoutMillis(Long.parseLong(properties.getProperty(S3_DOWNLOAD_TIMEOUT_MILLIS)));
    }

    if (properties.containsKey(LOCAL_DOWNLOAD_HTTP_DOWNLOAD_PATH)) {
      setLocalDownloadPath(properties.getProperty(LOCAL_DOWNLOAD_HTTP_DOWNLOAD_PATH));
    }

    if (properties.containsKey(LOCAL_DOWNLOAD_HTTP_PORT)) {
      setLocalDownloadHttpPort(Integer.parseInt(properties.getProperty(LOCAL_DOWNLOAD_HTTP_PORT)));
    }
  }
}
