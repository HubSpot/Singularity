package com.hubspot.singularity.executor.models;

import com.google.common.base.Objects;
import com.google.common.base.Optional;

public class LogrotateCompressionSettings {
  private Optional<String> compressCmd = Optional.absent();
  private Optional<String> uncompressCmd = Optional.absent();
  private Optional<String> compressOptions = Optional.absent();
  private Optional<String> compressExt = Optional.absent();

  public LogrotateCompressionSettings(Optional<String> compressCmd, Optional<String> uncompressCmd, Optional<String> compressOptions, Optional<String> compressExt) {
    this.compressCmd = compressCmd;
    this.uncompressCmd = uncompressCmd;
    this.compressOptions = compressOptions;
    this.compressExt = compressExt;
  }

  public static LogrotateCompressionSettings gzip() {
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
