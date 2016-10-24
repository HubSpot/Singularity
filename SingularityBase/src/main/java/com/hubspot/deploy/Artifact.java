package com.hubspot.deploy;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Artifact {

  private final String name;
  private final String filename;
  private final Optional<String> md5sum;
  private final Optional<String> targetFolderRelativeToTask;

  public Artifact(String name, String filename, Optional<String> md5sum, Optional<String> targetFolderRelativeToTask) {
    this.name = name;
    this.filename = filename;
    this.md5sum = md5sum;
    this.targetFolderRelativeToTask = targetFolderRelativeToTask;
  }

  public String getName() {
    return name;
  }

  public String getFilename() {
    return filename;
  }

  @JsonIgnore
  public String getFilenameForCache() {
    if (md5sum.isPresent()) {
      return String.format("%s-%s", md5sum.get(), filename);
    } else {
      return filename;
    }
  }

  public Optional<String> getMd5sum() {
    return md5sum;
  }

  public Optional<String> getTargetFolderRelativeToTask() {
    return targetFolderRelativeToTask;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, filename, md5sum, targetFolderRelativeToTask);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || other.getClass() != this.getClass()) {
      return false;
    }

    Artifact that = (Artifact) other;

    return Objects.equals(this.name, that.name)
        && Objects.equals(this.filename, that.filename)
        && Objects.equals(this.md5sum, that.md5sum)
        && Objects.equals(this.targetFolderRelativeToTask, that.targetFolderRelativeToTask);
  }

  @Override
  public String toString() {
    return "Artifact [name=" + name + ", filename=" + filename + ", md5sum=" + md5sum + ", targetFolderRelativeToTask=" + targetFolderRelativeToTask + "]";
  }

}
