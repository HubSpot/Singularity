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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((directory == null) ? 0 : directory.hashCode());
    result = prime * result + ((fileGlob == null) ? 0 : fileGlob.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    S3UploadMetadata other = (S3UploadMetadata) obj;
    if (directory == null) {
      if (other.directory != null)
        return false;
    } else if (!directory.equals(other.directory))
      return false;
    if (fileGlob == null) {
      if (other.fileGlob != null)
        return false;
    } else if (!fileGlob.equals(other.fileGlob))
      return false;
    return true;
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
