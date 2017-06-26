package com.hubspot.deploy;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
public interface ArtifactListIF {

  List<EmbeddedArtifact> getEmbeddedArtifacts();

  List<ExternalArtifact> getExternalArtifacts();

  List<S3ArtifactBase> getS3Artifacts();

  List<S3ArtifactBaseSignature> getS3ArtifactSignatures();

}
