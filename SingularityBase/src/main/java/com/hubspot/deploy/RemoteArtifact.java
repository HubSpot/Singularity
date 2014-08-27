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

}
