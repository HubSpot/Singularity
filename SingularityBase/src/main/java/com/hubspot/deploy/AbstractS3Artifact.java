package com.hubspot.deploy;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
@JsonDeserialize(as = S3Artifact.class)
public abstract class AbstractS3Artifact extends AbstractRemoteArtifact {

  @Parameter(order = 5)
  public abstract String getS3Bucket();

  @Parameter(order = 6)
  public abstract String getS3ObjectKey();

}
