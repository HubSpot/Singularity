package com.hubspot.singularity.runner.base.configuration;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.runner.base.constraints.DirectoryExists;

@Configuration(filename = "/etc/singularity.base.yaml", consolidatedField = "base")
public class SingularityRunnerBaseConfiguration extends BaseRunnerConfiguration {
  @DirectoryExists
  @JsonProperty
  private String s3UploaderMetadataDirectory;

  @NotEmpty
  @JsonProperty
  private String s3UploaderMetadataSuffix = ".s3.json";

  @DirectoryExists
  @JsonProperty
  private String logWatcherMetadataDirectory;

  @NotEmpty
  @JsonProperty
  private String logWatcherMetadataSuffix = ".tail.json";

  @NotNull
  @JsonProperty
  private Optional<String> sentryDsn = Optional.absent();

  @NotNull
  @JsonProperty
  private String sentryPrefix = "";

  @NotNull
  @JsonProperty
  private Optional<String> useCompressProgram = Optional.absent();

  public SingularityRunnerBaseConfiguration() {
    super(Optional.<String>absent());
    this.setLoggingDirectory(Optional.of("/var/log/singularity/"));
  }

  public String getS3UploaderMetadataDirectory() {
    return s3UploaderMetadataDirectory;
  }

  public void setS3UploaderMetadataDirectory(String s3UploaderMetadataDirectory) {
    this.s3UploaderMetadataDirectory = s3UploaderMetadataDirectory;
  }

  public String getS3UploaderMetadataSuffix() {
    return s3UploaderMetadataSuffix;
  }

  public void setS3UploaderMetadataSuffix(String s3UploaderMetadataSuffix) {
    this.s3UploaderMetadataSuffix = s3UploaderMetadataSuffix;
  }

  public String getLogWatcherMetadataDirectory() {
    return logWatcherMetadataDirectory;
  }

  public void setLogWatcherMetadataDirectory(String logWatcherMetadataDirectory) {
    this.logWatcherMetadataDirectory = logWatcherMetadataDirectory;
  }

  public String getLogWatcherMetadataSuffix() {
    return logWatcherMetadataSuffix;
  }

  public void setLogWatcherMetadataSuffix(String logWatcherMetadataSuffix) {
    this.logWatcherMetadataSuffix = logWatcherMetadataSuffix;
  }

  public Optional<String> getSentryDsn() {
    return sentryDsn;
  }

  public void setSentryDsn(Optional<String> sentryDsn) {
    this.sentryDsn = sentryDsn;
  }

  public String getSentryPrefix() {
    return sentryPrefix;
  }

  public void setSentryPrefix(String sentryPrefix) {
    this.sentryPrefix = sentryPrefix;
  }

  public Optional<String> getUseCompressProgram() {
    return useCompressProgram;
  }

  public void setUseCompressProgram(Optional<String> useCompressProgram) {
    this.useCompressProgram = useCompressProgram;
  }

  @Override
  public String toString() {
    return "SingularityRunnerBaseConfiguration[" +
            "s3UploaderMetadataDirectory='" + s3UploaderMetadataDirectory + '\'' +
            ", s3UploaderMetadataSuffix='" + s3UploaderMetadataSuffix + '\'' +
            ", logWatcherMetadataDirectory='" + logWatcherMetadataDirectory + '\'' +
            ", logWatcherMetadataSuffix='" + logWatcherMetadataSuffix + '\'' +
            ", useCompressProgram=" + useCompressProgram +
            ']';
  }
}
