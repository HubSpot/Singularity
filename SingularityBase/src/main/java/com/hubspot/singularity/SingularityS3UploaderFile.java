package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityS3UploaderFile {
  private final String filename;
  private final Optional<String> s3UploaderBucket;
  private final Optional<String> s3UploaderKeyPattern;
  private final Optional<String> s3UploaderFilenameHint;
  private final Optional<String> directory;
  private final Optional<String> s3StorageClass;
  private final Optional<Long> applyS3StorageClassAfterBytes;
  private final boolean checkSubdirectories;

  @JsonCreator
  public static SingularityS3UploaderFile fromString(String value) {
    return new SingularityS3UploaderFile(value, Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent());
  }

  @JsonCreator
  public SingularityS3UploaderFile(@JsonProperty("filename") String filename,
                                   @JsonProperty("s3UploaderBucket") Optional<String> s3UploaderBucket,
                                   @JsonProperty("s3UploaderKeyPattern") Optional<String> s3UploaderKeyPattern,
                                   @JsonProperty("s3UploaderFilenameHint") Optional<String> s3UploaderFilenameHint,
                                   @JsonProperty("directory") Optional<String> directory,
                                   @JsonProperty("s3StorageClass") Optional<String> s3StorageClass,
                                   @JsonProperty("applyS3StorageClassAfterBytes") Optional<Long> applyS3StorageClassAfterBytes,
                                   @JsonProperty("checkSubdirectories") Optional<Boolean> checkSubdirectories) {
    this.filename = filename;
    this.s3UploaderBucket = s3UploaderBucket;
    this.s3UploaderKeyPattern = s3UploaderKeyPattern;
    this.s3UploaderFilenameHint = s3UploaderFilenameHint;
    this.directory = directory;
    this.s3StorageClass = s3StorageClass;
    this.applyS3StorageClassAfterBytes = applyS3StorageClassAfterBytes;
    this.checkSubdirectories = checkSubdirectories.or(false);
  }

  public String getFilename() {
    return filename;
  }

  public Optional<String> getS3UploaderBucket() {
    return s3UploaderBucket;
  }

  public Optional<String> getS3UploaderKeyPattern() {
    return s3UploaderKeyPattern;
  }

  public Optional<String> getS3UploaderFilenameHint() {
    return s3UploaderFilenameHint;
  }

  public Optional<String> getDirectory() {
    return directory;
  }

  public Optional<String> getS3StorageClass() {
    return s3StorageClass;
  }

  public Optional<Long> getApplyS3StorageClassAfterBytes() {
    return applyS3StorageClassAfterBytes;
  }

  public boolean isCheckSubdirectories() {
    return checkSubdirectories;
  }

  @Override
  public String toString() {
    return "SingularityS3UploaderFile{" +
        "filename='" + filename + '\'' +
        ", s3UploaderBucket=" + s3UploaderBucket +
        ", s3UploaderKeyPattern=" + s3UploaderKeyPattern +
        ", s3UploaderFilenameHint=" + s3UploaderFilenameHint +
        ", directory=" + directory +
        ", s3StorageClass=" + s3StorageClass +
        ", applyS3StorageClassAfterBytes=" + applyS3StorageClassAfterBytes +
        ", checkSubdirectories=" + checkSubdirectories +
        '}';
  }
}
