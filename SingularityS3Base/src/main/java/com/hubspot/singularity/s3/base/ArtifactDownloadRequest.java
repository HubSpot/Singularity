package com.hubspot.singularity.s3.base;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.hubspot.deploy.S3Artifact;

public class ArtifactDownloadRequest {

  private final String targetDirectory;
  private final S3Artifact s3Artifact;

  @JsonCreator
  public ArtifactDownloadRequest(@JsonProperty("targetDirectory") String targetDirectory, @JsonProperty("s3Artifact") S3Artifact s3Artifact) {
    Preconditions.checkNotNull(targetDirectory);
    Preconditions.checkNotNull(s3Artifact);

    this.targetDirectory = targetDirectory;
    this.s3Artifact = s3Artifact;
  }

  public String getTargetDirectory() {
    return targetDirectory;
  }

  public S3Artifact getS3Artifact() {
    return s3Artifact;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((s3Artifact == null) ? 0 : s3Artifact.hashCode());
    result = prime * result + ((targetDirectory == null) ? 0 : targetDirectory.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ArtifactDownloadRequest other = (ArtifactDownloadRequest) obj;
    if (s3Artifact == null) {
      if (other.s3Artifact != null) {
        return false;
      }
    } else if (!s3Artifact.equals(other.s3Artifact)) {
      return false;
    }
    if (targetDirectory == null) {
      if (other.targetDirectory != null) {
        return false;
      }
    } else if (!targetDirectory.equals(other.targetDirectory)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "ArtifactDownloadRequest [targetDirectory=" + targetDirectory + ", s3Artifact=" + s3Artifact + "]";
  }

}
