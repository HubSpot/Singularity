package com.hubspot.singularity.runner.base.configuration;

import java.util.Properties;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

@Configuration("/etc/singularity.base.yaml")
public class SingularityRunnerBaseConfiguration extends BaseRunnerConfiguration {
  public static final String ROOT_LOG_DIRECTORY = "root.log.directory";

  public static final String LOG_METADATA_DIRECTORY = "logwatcher.metadata.directory";
  public static final String LOG_METADATA_SUFFIX = "logwatcher.metadata.suffix";

  public static final String S3_METADATA_SUFFIX = "s3uploader.metadata.suffix";
  public static final String S3_METADATA_DIRECTORY = "s3uploader.metadata.directory";

  @JsonProperty
  private String s3UploaderMetadataDirectory;

  @NotEmpty
  private String s3UploaderMetadataSuffix = ".s3.json";

  @JsonProperty
  private String logWatcherMetadataDirectory;

  @NotEmpty
  private String logWatcherMetadataSuffix = ".tail.json";

  public SingularityRunnerBaseConfiguration() {
    super(Optional.<String>absent());
    setLogging(SingularityRunnerBaseLoggingConfiguration.defaultBaseConfig());
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
