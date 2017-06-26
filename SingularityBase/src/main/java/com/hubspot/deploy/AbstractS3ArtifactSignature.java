package com.hubspot.deploy;

import org.immutables.value.Value.Immutable;

import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;
import com.wordnik.swagger.annotations.ApiModel;

@Immutable
@SingularityStyle
@ApiModel(description="A file with name `filename` containing the signature (e.g. gpg signature) for an artifact with the specified `artifactFilename`. Used to verify the validity of the artifact being downloaded")
public abstract class AbstractS3ArtifactSignature extends AbstractS3Artifact {
  public abstract String getName();

  public abstract String getFilename();

  public abstract Optional<String> getMd5sum();

  public abstract Optional<String> getTargetFolderRelativeToTask();

  public abstract String getArtifactFilename();
}
