package com.hubspot.deploy;

import org.immutables.value.Value.Immutable;

import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
public abstract class AbstractS3Artifact extends AbstractRemoteArtifact {

  public abstract String getS3Bucket();

  public abstract String getS3ObjectKey();

}
