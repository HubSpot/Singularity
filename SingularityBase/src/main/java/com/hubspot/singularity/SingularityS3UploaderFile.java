package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Describes a file that shouldbe uploaded by the SingularityS3Uploader")
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

  @Schema(description = "The name of the file")
  public String getFilename() {
    return filename;
  }

  @Schema(
      title = "The s3 bucket to upload to",
      nullable = true,
      defaultValue = "The default bucket configured in the S3Uploader"
  )
  public Optional<String> getS3UploaderBucket() {
    return s3UploaderBucket;
  }

  @Schema(
      title = "The pattern to use when generating the S3 key for the object that is uploaded",
      nullable = true,
      defaultValue = "The default pattern configured in the S3Uploader"
  )
  public Optional<String> getS3UploaderKeyPattern() {
    return s3UploaderKeyPattern;
  }

  @Schema(
      title = "An optional suffix appended to the uploader metadata file for easier debugging",
      nullable = true
  )
  public Optional<String> getS3UploaderFilenameHint() {
    return s3UploaderFilenameHint;
  }

  @Schema(
      title = "The directory to search for files to upload",
      nullable = true,
      defaultValue = "The task sandbox directory"
  )
  public Optional<String> getDirectory() {
    return directory;
  }

  @Schema(
      title = "The aws storage class to use for files uploaded",
      nullable = true,
      defaultValue = "The storage class as configured in the S3Uploader depending on file size"
  )
  public Optional<String> getS3StorageClass() {
    return s3StorageClass;
  }

  @Schema(
      title = "The size of file required in order to apply the `s3StorageClass` if present",
      nullable = true,
      defaultValue = "`applyS3StorageClassAfterBytes` as configured in the S3Uploader"
  )
  public Optional<Long> getApplyS3StorageClassAfterBytes() {
    return applyS3StorageClassAfterBytes;
  }

  @Schema(
      title = "Recursively check directories for matching files",
      defaultValue = "false"
  )
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
