package com.hubspot.deploy;

import com.google.common.base.Optional;

public abstract class RemoteArtifact extends Artifact {

  private final Optional<Long> filesize;

  public RemoteArtifact(String name, String filename, Optional<String> md5sum, Optional<Long> filesize) {
    super(name, filename, md5sum);
    this.filesize = filesize;
  }

  public Optional<Long> getFilesize() {
    return filesize;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((filesize == null) ? 0 : filesize.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    RemoteArtifact other = (RemoteArtifact) obj;
    if (filesize == null) {
      if (other.filesize != null) {
        return false;
      }
    } else if (!filesize.equals(other.filesize)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "RemoteArtifact [filesize=" + filesize + "]";
  }

}
