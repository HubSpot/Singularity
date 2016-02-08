package com.hubspot.deploy;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class S3ArtifactSignature extends S3Artifact {

  private final String artifactFilename;

  @JsonCreator
  public S3ArtifactSignature(@JsonProperty("name") String name, @JsonProperty("filename") String filename, @JsonProperty("md5sum") Optional<String> md5sum,
      @JsonProperty("filesize") Optional<Long> filesize, @JsonProperty("s3Bucket") String s3Bucket, @JsonProperty("s3ObjectKey") String s3ObjectKey,
      @JsonProperty("artifactFilename") String artifactFilename, @JsonProperty("targetFolderRelativeToTask") Optional<String> targetFolderRelativeToTask) {
    super(name, filename, md5sum, filesize, s3Bucket, s3ObjectKey, targetFolderRelativeToTask);

    this.artifactFilename = artifactFilename;
  }

  public String getArtifactFilename() {
    return artifactFilename;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    S3ArtifactSignature that = (S3ArtifactSignature) o;
    return Objects.equals(artifactFilename, that.artifactFilename);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), artifactFilename);
  }

  @Override
  public String toString() {
    return "S3ArtifactSignature [artifactFilename=" + artifactFilename + ", parent=" + super.toString() + "]";
  }

}
