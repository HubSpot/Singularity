package com.hubspot.singularity.runner.base.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class SingularityRunnerBaseConfiguration {

  private final Path metadataDirectory;
  private final String metadataSuffix;
  
  @Inject
  public SingularityRunnerBaseConfiguration(@Named(SingularityRunnerBaseConfigurationLoader.METADATA_DIRECTORY) String metadataDirectory, @Named(SingularityRunnerBaseConfigurationLoader.METADATA_SUFFIX) String metadataSuffix) {
    this.metadataDirectory = Paths.get(metadataDirectory);
    this.metadataSuffix = metadataSuffix;
  }

  public Path getMetadataDirectory() {
    return metadataDirectory;
  }

  public String getMetadataSuffix() {
    return metadataSuffix;
  }

  public Path getTailMetadataPath(TailMetadata tail) {
    return getMetadataDirectory().resolve(Paths.get(tail.getFilenameKey() + getMetadataSuffix()));
  }
  
  @Override
  public String toString() {
    return "SingularityRunnerBaseConfiguration [metadataDirectory=" + metadataDirectory + ", metadataSuffix=" + metadataSuffix + "]";
  }
  
}
