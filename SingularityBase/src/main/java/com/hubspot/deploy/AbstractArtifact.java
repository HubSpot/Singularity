package com.hubspot.deploy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Optional;

public abstract class AbstractArtifact {

  public abstract String getName();

  public abstract String getFilename();

  public abstract Optional<String> getMd5sum();

  public abstract Optional<String> getTargetFolderRelativeToTask();

  @JsonIgnore
  public String getFilenameForCache() {
    if (getMd5sum().isPresent()) {
      return String.format("%s-%s", getMd5sum().get(), getFilename());
    } else {
      return getFilename();
    }
  }
}
