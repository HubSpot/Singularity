package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularitySandboxFile extends SingularityJsonObject {

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

  public String getName() {
    return name;
  }

  public long getMtime() {
    return mtime;
  }

  public long getSize() {
    return size;
  }

  public String getMode() {
    return mode;
  }

  @Override
  public String toString() {
    return "SingularitySandboxFile [name=" + name + ", mtime=" + mtime + ", size=" + size + ", mode=" + mode + "]";
  }

}
