package com.hubspot.singularity.runner.base.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class S3UploadMetadata {
  
  private final String directory;
  private final String fileGlob;
  private final String s3Bucket;
  private final String s3KeyFormat;
  
  @JsonCreator
  public S3UploadMetadata(@JsonProperty("directory") String directory, @JsonProperty("fileGlob") String fileGlob, @JsonProperty("s3Bucket") String s3Bucket, @JsonProperty("s3KeyFormat") String s3KeyFormat) {
    this.directory = directory;
    this.fileGlob = fileGlob;
    this.s3Bucket = s3Bucket;
    this.s3KeyFormat = s3KeyFormat;
  }

  public String getDirectory() {
    return directory;
  }

  public String getFileGlob() {
    return fileGlob;
  }

  public String getS3Bucket() {
    return s3Bucket;
  }

  public String getS3KeyFormat() {
    return s3KeyFormat;
  }

  @Override
  public String toString() {
    return "S3UploadMetadata [directory=" + directory + ", fileGlob=" + fileGlob + ", s3Bucket=" + s3Bucket + ", s3KeyFormat=" + s3KeyFormat + "]";
  }
  
}
