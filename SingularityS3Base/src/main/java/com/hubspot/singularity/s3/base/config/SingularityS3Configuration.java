package com.hubspot.singularity.s3.base.config;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.runner.base.configuration.BaseRunnerConfiguration;
import com.hubspot.singularity.runner.base.configuration.Configuration;
import com.hubspot.singularity.runner.base.constraints.DirectoryExists;
import com.hubspot.singularity.runner.base.jackson.Obfuscate;

@Configuration(filename = "/etc/singularity.s3base.yaml", consolidatedField = "s3")
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
  @JsonProperty
  private String artifactCacheDirectory;

  @NotNull
  @Obfuscate
  @JsonProperty
  private String s3AccessKey = "";

  @NotNull
  @Obfuscate
  @JsonProperty
  private String s3SecretKey = "";

  @Min(1)
  @JsonProperty
  private long s3ChunkSize = 104857600;

  @Min(1)
  @JsonProperty
  private long s3DownloadTimeoutMillis = TimeUnit.MINUTES.toMillis(2);

  @Min(1)
  @JsonProperty
  private long s3ChunkDownloadTimeoutMillis = TimeUnit.SECONDS.toMillis(30);

  @Min(1)
  @JsonProperty
  private long s3ChunkRetries = 3;

  @Min(0)
  @JsonProperty
  private int localDownloadHttpPort = 7070;

  @NotEmpty
  @JsonProperty
  private String localDownloadPath = "/download";


  @NotEmpty
  @JsonProperty
  private boolean gpgCheckingEnabled = false;

  @JsonProperty
  private String gpgBinaryPath = "/usr/bin/gpg";

  @Obfuscate
  @JsonProperty
  private String gpgKeyPassword;

  @JsonProperty
  private String gpgHome;

  @JsonProperty
  private String gpgKeyUsername;

  public SingularityS3Configuration() {
    super(Optional.<String>absent());
  }

  public String getArtifactCacheDirectory() {
    return artifactCacheDirectory;
  }

  public void setArtifactCacheDirectory(String artifactCacheDirectory) {
    this.artifactCacheDirectory = artifactCacheDirectory;
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

  public long getS3ChunkRetries() {
    return s3ChunkRetries;
  }

  public void setS3ChunkRetries(long s3ChunkRetries) {
    this.s3ChunkRetries = s3ChunkRetries;
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

  public long getS3ChunkDownloadTimeoutMillis() {
    return s3ChunkDownloadTimeoutMillis;
  }

  public void setS3ChunkDownloadTimeoutMillis(long s3ChunkDownloadTimeoutMillis) {
    this.s3ChunkDownloadTimeoutMillis = s3ChunkDownloadTimeoutMillis;
  }

  public boolean isGpgCheckingEnabled() {
    return gpgCheckingEnabled;
  }

  public void setGpgCheckingEnabled(boolean gpgCheckingEnabled) {
    this.gpgCheckingEnabled = gpgCheckingEnabled;
  }

  public String getGpgBinaryPath() {
    return gpgBinaryPath;
  }

  public void setGpgBinaryPath(String gpgBinaryPath) {
    this.gpgBinaryPath = gpgBinaryPath;
  }

  public String getGpgHome() {
    return gpgHome;
  }

  public void setGpgHome(String gpgHome) {
    this.gpgHome = gpgHome;
  }

  public String getGpgKeyPassword() {
    return gpgKeyPassword;
  }

  public void setGpgKeyPassword(String gpgKeyPassword) {
    this.gpgKeyPassword = gpgKeyPassword;
  }

  public String getGpgKeyUsername() {
    return gpgKeyUsername;
  }

  public void setGpgKeyUsername(String gpgKeyUsername) {
    this.gpgKeyUsername = gpgKeyUsername;
  }

  @Override
  public String toString() {
    return "SingularityS3Configuration [artifactCacheDirectory=" + artifactCacheDirectory + ", s3AccessKey=" + s3AccessKey + ", s3SecretKey=" + s3SecretKey + ", s3ChunkSize=" + s3ChunkSize
        + ", s3DownloadTimeoutMillis=" + s3DownloadTimeoutMillis + ", s3ChunkDownloadTimeoutMillis=" + s3ChunkDownloadTimeoutMillis + ", s3ChunkRetries=" + s3ChunkRetries + ", localDownloadHttpPort="
        + localDownloadHttpPort + ", localDownloadPath=" + localDownloadPath + ", gpgBinaryPath=" + gpgBinaryPath + ", gpgKeyPassword=" + gpgKeyPassword + ", gpgHome=" + gpgHome + ", gpgKeyUsername="
        + gpgKeyUsername + "]";
  }

  @Override
  public void updateFromProperties(Properties properties) {
    if (properties.containsKey(ARTIFACT_CACHE_DIRECTORY)) {
      setArtifactCacheDirectory(properties.getProperty(ARTIFACT_CACHE_DIRECTORY));
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
