package com.hubspot.singularity.runner.base.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.runner.base.shared.TailMetadata;

public class SingularityRunnerBaseConfiguration {

  private final Path logMetadataDirectory;
  private final String logMetadataSuffix;
  private final Path s3MetadataDirectory;
  private final String s3MetadataSuffix;
  
  @Inject
  public SingularityRunnerBaseConfiguration(@Named(SingularityRunnerBaseConfigurationLoader.LOG_METADATA_DIRECTORY) String logMetadataDirectory, @Named(SingularityRunnerBaseConfigurationLoader.LOG_METADATA_SUFFIX) String logMetadataSuffix,
      @Named(SingularityRunnerBaseConfigurationLoader.S3_METADATA_DIRECTORY) String s3MetadataDirectory, @Named(SingularityRunnerBaseConfigurationLoader.S3_METADATA_DIRECTORY) String s3MetadataSuffix) {
    this.logMetadataDirectory = Paths.get(logMetadataDirectory);
    this.logMetadataSuffix = logMetadataSuffix;
    this.s3MetadataDirectory = Paths.get(s3MetadataDirectory);
    this.s3MetadataSuffix = s3MetadataSuffix;
  }

  public Path getLogMetadataDirectory() {
    return logMetadataDirectory;
  }

  public String getLogMetadataSuffix() {
    return logMetadataSuffix;
  }

  public Path getS3MetadataDirectory() {
    return s3MetadataDirectory;
  }

  public String getS3MetadataSuffix() {
    return s3MetadataSuffix;
  }

  public Path getTailMetadataPath(TailMetadata tail) {
    return getLogMetadataDirectory().resolve(Paths.get(tail.getFilenameKey() + getLogMetadataSuffix()));
  }

  @Override
  public String toString() {
    return "SingularityRunnerBaseConfiguration [logMetadataDirectory=" + logMetadataDirectory + ", logMetadataSuffix=" + logMetadataSuffix + ", s3MetadataDirectory=" + s3MetadataDirectory + ", s3MetadataSuffix=" + s3MetadataSuffix + "]";
  }
  
  
}
