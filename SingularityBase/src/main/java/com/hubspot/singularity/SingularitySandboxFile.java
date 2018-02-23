package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(title = "Represents a file in a Mesos sandbox")
public class SingularitySandboxFile {

  private final String name;
  private final long mtime;
  private final long size;
  private final String mode;

  @JsonCreator
  public SingularitySandboxFile(@JsonProperty("name") String name, @JsonProperty("mtime") long mtime, @JsonProperty("size") long size, @JsonProperty("mode") String mode) {
    this.mode = mode;
    this.name = name;
    this.mtime = mtime;
    this.size = size;
  }

  @Schema(title = "Filename")
  public String getName() {
    return name;
  }

  @Schema(title = "Last modified time")
  public long getMtime() {
    return mtime;
  }

  @Schema(title = "File size (in bytes)")
  public long getSize() {
    return size;
  }

  @Schema(title = "File mode")
  public String getMode() {
    return mode;
  }

  @Override
  public String toString() {
    return "SingularitySandboxFile{" +
        "name='" + name + '\'' +
        ", mtime=" + mtime +
        ", size=" + size +
        ", mode='" + mode + '\'' +
        '}';
  }
}
