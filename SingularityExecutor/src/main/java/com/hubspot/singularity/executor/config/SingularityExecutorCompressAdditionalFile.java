package com.hubspot.singularity.executor.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.singularity.runner.base.shared.CompressionType;
import java.util.Optional;

public class SingularityExecutorCompressAdditionalFile {
  private final Optional<String> directory;
  private final String filenameGlob;
  private final CompressionType compressionType;

  @JsonCreator
  public SingularityExecutorCompressAdditionalFile(
    @JsonProperty("directory") String directory,
    @JsonProperty("filenameGlob") String filenameGlob,
    @JsonProperty("compressionType") CompressionType compressionType
  ) {
    this.directory = Optional.ofNullable(directory);
    this.filenameGlob = filenameGlob;
    this.compressionType = compressionType;
  }

  public Optional<String> getDirectory() {
    return directory;
  }

  public String getFilenameGlob() {
    return filenameGlob;
  }

  public CompressionType getCompressionType() {
    return compressionType;
  }
}
