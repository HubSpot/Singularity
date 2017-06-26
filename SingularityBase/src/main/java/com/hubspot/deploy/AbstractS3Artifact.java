package com.hubspot.deploy;

import org.immutables.value.Value.Immutable;

import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
public abstract class AbstractS3Artifact extends S3ArtifactBase {

  public abstract String getName();

  public abstract String getFilename();

  public abstract Optional<String> getMd5sum();

  public abstract Optional<String> getTargetFolderRelativeToTask();

  public abstract Optional<Long> filesize();

  public boolean isArtifactList() {
    return false;
  }

  public abstract String getS3Bucket();

  public abstract String getS3ObjectKey();

}
