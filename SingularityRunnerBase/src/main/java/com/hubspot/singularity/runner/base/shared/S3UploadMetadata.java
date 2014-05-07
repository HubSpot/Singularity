package com.hubspot.singularity.runner.base.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

/**
 * s3KeyFormat is the format for the S3 file.
 * 
 * It can contain the following:
 * %filename - adds the original file's filename
 * %fileext - adds the original file's file ext
 * %Y - adds year
 * %m - adds month
 * %d - adds day of the month 
 * %s - adds milliseconds
 * %index - adds the index of the file uploaded at this moment (to preserve uniqueness)
 *
 */
public class S3UploadMetadata {
  
  private final String directory;
  private final String fileGlob;
  private final String s3Bucket;
  private final String s3KeyFormat;
  private final boolean finished;
  
  @JsonCreator
  public S3UploadMetadata(@JsonProperty("directory") String directory, @JsonProperty("fileGlob") String fileGlob, @JsonProperty("s3Bucket") String s3Bucket, @JsonProperty("s3KeyFormat") String s3KeyFormat, @JsonProperty("finished") boolean finished) {
    Preconditions.checkNotNull(directory);
    Preconditions.checkNotNull(fileGlob);
    Preconditions.checkNotNull(s3Bucket);
    Preconditions.checkNotNull(s3KeyFormat);
    
    this.directory = directory;
    this.fileGlob = fileGlob;
    this.s3Bucket = s3Bucket;
    this.s3KeyFormat = s3KeyFormat;
    this.finished = finished;
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

  public boolean isFinished() {
    return finished;
  }

  @Override
  public String toString() {
    return "S3UploadMetadata [directory=" + directory + ", fileGlob=" + fileGlob + ", s3Bucket=" + s3Bucket + ", s3KeyFormat=" + s3KeyFormat + ", finished=" + finished + "]";
  }
  
}
