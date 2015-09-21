package com.hubspot.singularity.runner.base.configuration;

import java.util.Properties;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.runner.base.constraints.DirectoryExists;

@Configuration(filename = "/etc/singularity.base.yaml", consolidatedField = "base")
public class SingularityRunnerBaseConfiguration extends BaseRunnerConfiguration {
  public static final String ROOT_LOG_DIRECTORY = "root.log.directory";

  public static final String LOG_METADATA_DIRECTORY = "logwatcher.metadata.directory";
  public static final String LOG_METADATA_SUFFIX = "logwatcher.metadata.suffix";

  public static final String S3_METADATA_SUFFIX = "s3uploader.metadata.suffix";
  public static final String S3_METADATA_DIRECTORY = "s3uploader.metadata.directory";

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

  @Override
  public String toString() {
    return "SingularityRunnerBaseConfiguration[" +
            "s3UploaderMetadataDirectory='" + s3UploaderMetadataDirectory + '\'' +
            ", s3UploaderMetadataSuffix='" + s3UploaderMetadataSuffix + '\'' +
            ", logWatcherMetadataDirectory='" + logWatcherMetadataDirectory + '\'' +
            ", logWatcherMetadataSuffix='" + logWatcherMetadataSuffix + '\'' +
            ']';
  }

  @Override
  public void updateFromProperties(Properties properties) {
    if (properties.containsKey(LOG_METADATA_DIRECTORY)) {
      setLogWatcherMetadataDirectory(properties.getProperty(LOG_METADATA_DIRECTORY));
    }

    if (properties.containsKey(LOG_METADATA_SUFFIX)) {
      setLogWatcherMetadataSuffix(properties.getProperty(LOG_METADATA_SUFFIX));
    }

    if (properties.containsKey(S3_METADATA_DIRECTORY)) {
      setS3UploaderMetadataDirectory(properties.getProperty(S3_METADATA_DIRECTORY));
    }

    if (properties.containsKey(S3_METADATA_SUFFIX)) {
      setS3UploaderMetadataSuffix(properties.getProperty(S3_METADATA_SUFFIX));
    }
  }

}
