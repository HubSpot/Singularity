package com.hubspot.deploy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class S3Artifact extends RemoteArtifact {

  private final String s3Bucket;
  private final String s3ObjectKey;

  @JsonCreator
  public S3Artifact(@JsonProperty("name") String name, @JsonProperty("filename") String filename, @JsonProperty("md5sum") Optional<String> md5sum, @JsonProperty("filesize") Optional<Long> filesize,
      @JsonProperty("s3Bucket") String s3Bucket, @JsonProperty("s3ObjectKey") String s3ObjectKey) {
    super(name, filename, md5sum, filesize);
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
  public String toString() {
    return "S3Artifact [s3Bucket=" + s3Bucket + ", s3ObjectKey=" + s3ObjectKey + ", getFilesize()=" + getFilesize() + ", getName()=" + getName() + ", getFilename()=" + getFilename()
        + ", getMd5sum()=" + getMd5sum() + "]";
  }

}
