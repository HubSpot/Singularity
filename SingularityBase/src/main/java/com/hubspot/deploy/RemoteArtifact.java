package com.hubspot.deploy;

import java.util.Objects;

import com.google.common.base.Optional;

public abstract class RemoteArtifact extends Artifact {

  private final Optional<Long> filesize;

  public RemoteArtifact(String name, String filename, Optional<String> md5sum, Optional<Long> filesize, Optional<String> targetFolderRelativeToTask) {
    super(name, filename, md5sum, targetFolderRelativeToTask);
    this.filesize = filesize;
  }

  public Optional<Long> getFilesize() {
    return filesize;
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
    return Objects.equals(filesize, that.filesize);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), filesize);
  }

  @Override
  public String toString() {
    return "RemoteArtifact [filesize=" + filesize + ", parent=" + super.toString() + "]";
  }

}
