package com.hubspot.deploy;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
@JsonDeserialize(as = S3Artifact.class)
public abstract class AbstractS3Artifact extends AbstractRemoteArtifact {

  public abstract String getS3Bucket();

  public abstract String getS3ObjectKey();

}
