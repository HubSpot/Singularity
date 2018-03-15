package com.hubspot.singularity.executor.config;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LogrotateCompressionSettings {
  private Optional<String> compressCmd = Optional.empty();
  private Optional<String> uncompressCmd = Optional.empty();
  private Optional<String> compressOptions = Optional.empty();
  private Optional<String> compressExt = Optional.empty();

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
    return new LogrotateCompressionSettings(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
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
    return "LogrotateCompressionSettings{" +
        "compressCmd=" + compressCmd +
        ", uncompressCmd=" + uncompressCmd +
        ", compressOptions=" + compressOptions +
        ", compressExt=" + compressExt +
        '}';
  }
}
