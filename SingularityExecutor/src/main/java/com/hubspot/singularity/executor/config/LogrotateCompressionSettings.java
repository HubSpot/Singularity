package com.hubspot.singularity.executor.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

public class LogrotateCompressionSettings {
  private Optional<String> compressCmd = Optional.absent();
  private Optional<String> uncompressCmd = Optional.absent();
  private Optional<String> compressOptions = Optional.absent();
  private Optional<String> compressExt = Optional.absent();

  @JsonCreator
  public LogrotateCompressionSettings(@JsonProperty("compressCmd") Optional<String> compressCmd,
                                      @JsonProperty("uncompressCmd") Optional<String> uncompressCmd,
                                      @JsonProperty("compressOptions") Optional<String> compressOptions,
                                      @JsonProperty("compressExt") Optional<String> compressExt) {
    this.compressCmd = compressCmd;
    this.uncompressCmd = uncompressCmd;
    this.compressOptions = compressOptions;
    this.compressExt = compressExt;
  }

  public static LogrotateCompressionSettings empty() {
    return new LogrotateCompressionSettings(Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent());
  }

  public Optional<String> getCompressCmd() {
    return compressCmd;
  }

  public void setCompressCmd(Optional<String> compressCmd) {
    this.compressCmd = compressCmd;
  }

  public Optional<String> getUncompressCmd() {
    return uncompressCmd;
  }

  public void setUncompressCmd(Optional<String> uncompressCmd) {
    this.uncompressCmd = uncompressCmd;
  }

  public Optional<String> getCompressOptions() {
    return compressOptions;
  }

  public void setCompressOptions(Optional<String> compressOptions) {
    this.compressOptions = compressOptions;
  }

  public Optional<String> getCompressExt() {
    return compressExt;
  }

  public void setCompressExt(Optional<String> compressExt) {
    this.compressExt = compressExt;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("compressCmd", compressCmd)
      .add("uncompressCmd", uncompressCmd)
      .add("compressOptions", compressOptions)
      .add("compressExt", compressExt)
      .toString();
  }
}
