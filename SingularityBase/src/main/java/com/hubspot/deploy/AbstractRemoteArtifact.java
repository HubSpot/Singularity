package com.hubspot.deploy;

import org.immutables.value.Value.Immutable;

import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
public abstract class AbstractRemoteArtifact extends RemoteArtifactBase {
  public abstract String getName();

  public abstract String getFilename();

  public abstract Optional<String> getMd5sum();

  public abstract Optional<String> getTargetFolderRelativeToTask();

  public abstract Optional<Long> filesize();

  public boolean isArtifactList() {
    return false;
  }
}
