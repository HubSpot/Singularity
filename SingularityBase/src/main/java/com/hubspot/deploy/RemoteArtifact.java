package com.hubspot.deploy;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Optional;

public abstract class RemoteArtifact extends Artifact {

  private final Optional<Long> filesize;
  private final Optional<Boolean> isArtifactList;

  public RemoteArtifact(String name, String filename, Optional<String> md5sum, Optional<Long> filesize, Optional<String> targetFolderRelativeToTask, Optional<Boolean> isArtifactList) {
    super(name, filename, md5sum, targetFolderRelativeToTask);
    this.filesize = filesize;
    this.isArtifactList = isArtifactList;
  }

  public Optional<Long> getFilesize() {
    return filesize;
  }

  public Optional<Boolean> getIsArtifactList() {
    return isArtifactList;
  }

  @JsonIgnore
  public boolean isArtifactList() {
    return isArtifactList.or(Boolean.FALSE).booleanValue();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    RemoteArtifact that = (RemoteArtifact) o;
    return Objects.equals(filesize, that.filesize) &&
        Objects.equals(isArtifactList, that.isArtifactList);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), filesize, isArtifactList);
  }

  @Override
  public String toString() {
    return "RemoteArtifact{" +
        "filesize=" + filesize +
        "isArtifactList" + isArtifactList +
        "} " + super.toString();
  }
}
