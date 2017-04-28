package com.hubspot.deploy;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.mesos.JavaUtils;

public class ArtifactList {

  private final List<EmbeddedArtifact> embeddedArtifacts;
  private final List<ExternalArtifact> externalArtifacts;
  private final List<S3Artifact> s3Artifacts;
  private final List<S3ArtifactSignature> s3ArtifactSignatures;

  @JsonCreator
  public ArtifactList(@JsonProperty("embeddedArtifacts") List<EmbeddedArtifact> embeddedArtifacts,
      @JsonProperty("externalArtifacts") List<ExternalArtifact> externalArtifacts,
      @JsonProperty("s3Artifacts") List<S3Artifact> s3Artifacts,
      @JsonProperty("s3ArtifactSignatures") List<S3ArtifactSignature> s3ArtifactSignatures) {
    this.embeddedArtifacts = JavaUtils.nonNullImmutable(embeddedArtifacts);
    this.externalArtifacts = JavaUtils.nonNullImmutable(externalArtifacts);
    this.s3Artifacts = JavaUtils.nonNullImmutable(s3Artifacts);
    this.s3ArtifactSignatures = JavaUtils.nonNullImmutable(s3ArtifactSignatures);
  }

  public List<EmbeddedArtifact> getEmbeddedArtifacts() {
    return embeddedArtifacts;
  }

  public List<ExternalArtifact> getExternalArtifacts() {
    return externalArtifacts;
  }

  public List<S3Artifact> getS3Artifacts() {
    return s3Artifacts;
  }

  public List<S3ArtifactSignature> getS3ArtifactSignatures() {
    return s3ArtifactSignatures;
  }

}
