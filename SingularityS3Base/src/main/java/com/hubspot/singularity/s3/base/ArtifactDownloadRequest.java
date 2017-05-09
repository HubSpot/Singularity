package com.hubspot.singularity.s3.base;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.hubspot.deploy.S3Artifact;

public class ArtifactDownloadRequest {

  private final String targetDirectory;
  private final S3Artifact s3Artifact;
  private final Optional<Long> timeoutMillis;

  @JsonCreator
  public ArtifactDownloadRequest(@JsonProperty("targetDirectory") String targetDirectory, @JsonProperty("s3Artifact") S3Artifact s3Artifact,
      @JsonProperty("timeoutMillis") Optional<Long> timeoutMillis)  {
    Preconditions.checkNotNull(targetDirectory);
    Preconditions.checkNotNull(s3Artifact);

    this.targetDirectory = targetDirectory;
    this.s3Artifact = s3Artifact;
    this.timeoutMillis = timeoutMillis;
  }

  public String getTargetDirectory() {
    return targetDirectory;
  }

  public S3Artifact getS3Artifact() {
    return s3Artifact;
  }

  public Optional<Long> getTimeoutMillis() {
    return timeoutMillis;
  }

  @Override
  public int hashCode() {
    return Objects.hash(targetDirectory, s3Artifact);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ArtifactDownloadRequest that = (ArtifactDownloadRequest) o;
    return Objects.equals(s3Artifact, that.s3Artifact) &&
        Objects.equals(targetDirectory, that.targetDirectory);
  }

  @Override
  public String toString() {
    return "ArtifactDownloadRequest [targetDirectory=" + targetDirectory + ", s3Artifact=" + s3Artifact + ", timeoutMillis=" + timeoutMillis + "]";
  }

}
