package com.hubspot.deploy;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class S3Artifact extends RemoteArtifact {

  private final String s3Bucket;
  private final String s3ObjectKey;

  @JsonCreator
  public S3Artifact(@JsonProperty("name") String name, @JsonProperty("filename") String filename, @JsonProperty("md5sum") Optional<String> md5sum, @JsonProperty("filesize") Optional<Long> filesize,
      @JsonProperty("s3Bucket") String s3Bucket, @JsonProperty("s3ObjectKey") String s3ObjectKey, @JsonProperty("targetFolderRelativeToTask") Optional<String> targetFolderRelativeToTask) {
    super(name, filename, md5sum, filesize, targetFolderRelativeToTask);

    this.s3Bucket = s3Bucket;
    this.s3ObjectKey = s3ObjectKey;
  }

  public String getS3Bucket() {
    return s3Bucket;
  }

  public String getS3ObjectKey() {
    return s3ObjectKey;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), s3Bucket, s3ObjectKey);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    if (!super.equals(other)) {
      return false;
    }
    S3Artifact that = (S3Artifact) other;
    return Objects.equals(s3Bucket, that.s3Bucket) &&
        Objects.equals(s3ObjectKey, s3ObjectKey);
  }

  @Override
  public String toString() {
    return "S3Artifact{" +
        "s3Bucket='" + s3Bucket + '\'' +
        ", s3ObjectKey='" + s3ObjectKey + '\'' +
        "} " + super.toString();
  }
}
