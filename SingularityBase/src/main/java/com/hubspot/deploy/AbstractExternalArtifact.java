package com.hubspot.deploy;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
@JsonDeserialize(as = ExternalArtifact.class)
public abstract class AbstractExternalArtifact extends AbstractRemoteArtifact {

  public abstract String getName();

  public abstract String getFilename();

  public abstract String getUrl();

  public abstract Optional<String> getMd5sum();

  public abstract Optional<String> getTargetFolderRelativeToTask();

}
