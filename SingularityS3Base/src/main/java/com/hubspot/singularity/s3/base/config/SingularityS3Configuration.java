package com.hubspot.singularity.s3.base.config;

import java.util.HashMap;
import java.util.Map;
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
  @NotEmpty
  @DirectoryExists
  @JsonProperty
  private String artifactCacheDirectory;

  @NotNull
  @Obfuscate
  @JsonProperty
  private Optional<String> s3AccessKey = Optional.absent();

  @NotNull
  @Obfuscate
  @JsonProperty
  private Optional<String> s3SecretKey = Optional.absent();

  @Min(1)
  @JsonProperty
  private long s3ChunkSize = 104857600;

  @Min(1)
  @JsonProperty
  private long s3DownloadTimeoutMillis = TimeUnit.MINUTES.toMillis(2);

  @Min(1)
  @JsonProperty
  private int s3ChunkDownloadTimeoutMillis = (int) TimeUnit.SECONDS.toMillis(30);

  @Min(1)
  @JsonProperty
  private long s3ChunkRetries = 3;

  @Min(0)
  @JsonProperty
  private int localDownloadHttpPort = 7070;

  @NotEmpty
  @JsonProperty
  private String localDownloadPath = "/download";

  @NotNull
  @JsonProperty
  private Map<String, SingularityS3Credentials> s3BucketCredentials = new HashMap<>();

  public SingularityS3Configuration() {
    super(Optional.<String>absent());
  }

  public String getArtifactCacheDirectory() {
    return artifactCacheDirectory;
  }

  public void setArtifactCacheDirectory(String artifactCacheDirectory) {
    this.artifactCacheDirectory = artifactCacheDirectory;
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

  public int getS3ChunkDownloadTimeoutMillis() {
    return s3ChunkDownloadTimeoutMillis;
  }

  public void setS3ChunkDownloadTimeoutMillis(int s3ChunkDownloadTimeoutMillis) {
    this.s3ChunkDownloadTimeoutMillis = s3ChunkDownloadTimeoutMillis;
  }

  public Map<String, SingularityS3Credentials> getS3BucketCredentials() {
    return s3BucketCredentials;
  }

  public void setS3BucketCredentials(Map<String, SingularityS3Credentials> s3BucketCredentials) {
    this.s3BucketCredentials = s3BucketCredentials;
  }

  @Override
  public String toString() {
    return "SingularityS3Configuration{" +
        "artifactCacheDirectory='" + artifactCacheDirectory + '\'' +
        ", s3AccessKey=" + s3AccessKey +
        ", s3SecretKey=" + s3SecretKey +
        ", s3ChunkSize=" + s3ChunkSize +
        ", s3DownloadTimeoutMillis=" + s3DownloadTimeoutMillis +
        ", s3ChunkDownloadTimeoutMillis=" + s3ChunkDownloadTimeoutMillis +
        ", s3ChunkRetries=" + s3ChunkRetries +
        ", localDownloadHttpPort=" + localDownloadHttpPort +
        ", localDownloadPath='" + localDownloadPath + '\'' +
        ", s3BucketCredentials=" + s3BucketCredentials +
        "} " + super.toString();
  }
}
