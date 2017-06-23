package com.hubspot.deploy;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Immutable
@SingularityStyle
@JsonDeserialize(as = EmbeddedArtifact.class)
public abstract class AbstractEmbeddedArtifact extends AbstractArtifact {
  public abstract String getName();

  public abstract String getFilename();

  public abstract Optional<String> getMd5sum();

  public abstract Optional<String> getTargetFolderRelativeToTask();

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public abstract byte[] getContent();
}
