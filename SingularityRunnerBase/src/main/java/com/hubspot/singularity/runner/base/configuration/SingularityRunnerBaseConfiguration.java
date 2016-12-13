package com.hubspot.singularity.runner.base.configuration;

import java.util.Arrays;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.runner.base.constraints.DirectoryExists;

@Configuration(filename = "/etc/singularity.base.yaml", consolidatedField = "base")
public class SingularityRunnerBaseConfiguration extends BaseRunnerConfiguration {
  public static final String URI_PLACEHOLDER = "{URI}";
  public static final String SOURCE_FILENAME_PLACEHOLDER = "{SOURCE_FILENAME}";
  public static final String DESTINATION_FILENAME_PLACEHOLDER = "{DESTINATION_FILENAME}";

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
  @NotEmpty
  @JsonProperty
  private List<String> downloadUriCommand = Arrays.asList(
      "wget",
      URI_PLACEHOLDER,
      "-O",
      DESTINATION_FILENAME_PLACEHOLDER,
      "-nv",
      "--no-check-certificate");

  @NotNull
  @NotEmpty
  @JsonProperty
  private List<String> untarCommand = Arrays.asList(
      "tar",
      "-oxzf",
      SOURCE_FILENAME_PLACEHOLDER,
      "-C",
      DESTINATION_FILENAME_PLACEHOLDER);

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

  public List<String> getDownloadUriCommand() {
    return downloadUriCommand;
  }

  public void setDownloadUriCommand(List<String> downloadUriCommand) {
    this.downloadUriCommand = downloadUriCommand;
  }

  public List<String> getUntarCommand() {
    return untarCommand;
  }

  public void setUntarCommand(List<String> untarCommand) {
    this.untarCommand = untarCommand;
  }

  @Override
  public String toString() {
    return "SingularityRunnerBaseConfiguration{" +
        "s3UploaderMetadataDirectory='" + s3UploaderMetadataDirectory + '\'' +
        ", s3UploaderMetadataSuffix='" + s3UploaderMetadataSuffix + '\'' +
        ", logWatcherMetadataDirectory='" + logWatcherMetadataDirectory + '\'' +
        ", logWatcherMetadataSuffix='" + logWatcherMetadataSuffix + '\'' +
        ", sentryDsn=" + sentryDsn +
        ", sentryPrefix='" + sentryPrefix + '\'' +
        ", downloadUriCommand=" + downloadUriCommand +
        ", untarCommand=" + untarCommand +
        '}';
  }
}
