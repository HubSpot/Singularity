package com.hubspot.singularity.runner.base.shared;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 *
 * directory - the directory to watch for files inside of
 * fileGlob - only files matching this glob will be uploaded to S3.
 * s3Bucket - the name of the bucket to upload to in S3
 *
 * s3KeyFormat - the format for the actual key name for the object in S3 corresponding to each file uploaded. This can be dynamically
 * formatted with the following variables:
 *
 * %filename - adds the original file's filename
 * %fileext - adds the original file's file ext
 * %Y - adds year
 * %m - adds month
 * %d - adds day of the month
 * %s - adds milliseconds
 * %guid - adds a guid
 * %host - adds the hostname
 * %index - adds the index of the file uploaded at this moment (to preserve uniqueness)
 *
 * For example, if the s3KeyFormat was: %filename-%Y and the file name on local disk was "file1.txt" the S3 key would be : s3Bucket/file1.txt-2015 (assuming current year is 2015)
 *
 * finished - set this to true if you wish *this s3 upload metadata configuration* file to be deleted and no more files uploaded after the last matching file is uploaded to S3 successfully (think of it as safe delete.)
 * onFinishGlob - a glob to match files which should be uploaded *only* after finished is set to true OR the pid is no longer active
 * pid - the pid of the process to watch, such that when that pid is no longer running, finished is set to true (stop uploading files / watching directory once all files are successfully uploaded.)
 * s3AccessKey - the access key to use to talk to s3 (optional in case you want to re-use the default Singularity configuration's key)
 * s3SecretKey - the secret key to use to talk to s3 (optional in case you want to re-use the default Singularity configuration's key)
 *
 * finishedAfterMillisWithoutNewFile - after millis without a new file, set finished to true (see above for result.) - (-1 never expire) - absent - uses system default.
 * uploadImmediately - When detected, immediately upload to S3 rather than waiting for polling to upload
 *
 */
public class S3UploadMetadata {

  private final String directory;
  private final String fileGlob;
  private final String s3Bucket;
  private final String s3KeyFormat;
  private final boolean finished;
  private final Optional<String> onFinishGlob;
  private final Optional<Integer> pid;
  private final Optional<String> s3AccessKey;
  private final Optional<String> s3SecretKey;
  private final Optional<Long> finishedAfterMillisWithoutNewFile;
  private final Optional<String> s3StorageClass;
  private final Optional<Long> applyStorageClassIfOverBytes;
  private final Optional<Boolean> uploadImmediately;
  private final boolean checkSubdirectories;
  private final SingularityUploaderType uploaderType;
  private final Map<String, Object> gcsCredentials;
  private final Optional<String> gcsStorageClass;

  @JsonCreator
  public S3UploadMetadata(@JsonProperty("directory") String directory,
                          @JsonProperty("fileGlob") String fileGlob,
                          @JsonProperty("s3Bucket") String s3Bucket,
                          @JsonProperty("s3KeyFormat") String s3KeyFormat,
                          @JsonProperty("finished") boolean finished,
                          @JsonProperty("onFinishGlob") Optional<String> onFinishGlob,
                          @JsonProperty("pid") Optional<Integer> pid,
                          @JsonProperty("s3AccessKey") Optional<String> s3AccessKey,
                          @JsonProperty("s3SecretKey") Optional<String> s3SecretKey,
                          @JsonProperty("finishedAfterMillisWithoutNewFile") Optional<Long> finishedAfterMillisWithoutNewFile,
                          @JsonProperty("storageClass") Optional<String> s3StorageClass,
                          @JsonProperty("applyStorageClassIfOverBytes") Optional<Long> applyStorageClassIfOverBytes,
                          @JsonProperty("uploadImmediately") Optional<Boolean> uploadImmediately,
                          @JsonProperty("checkSubdirectories") Optional<Boolean> checkSubdirectories,
                          @JsonProperty("uploaderType") Optional<SingularityUploaderType> uploaderType,
                          @JsonProperty("gcsCredentials") Map<String, Object> gcsCredentials,
                          @JsonProperty("gcsStorageClass") Optional<String> gcsStorageClass) {
    Preconditions.checkNotNull(directory);
    Preconditions.checkNotNull(fileGlob);
    Preconditions.checkNotNull(s3Bucket);
    Preconditions.checkNotNull(s3KeyFormat);

    this.directory = directory;
    this.fileGlob = fileGlob;
    this.s3Bucket = s3Bucket;
    this.s3KeyFormat = s3KeyFormat;
    this.finished = finished;
    this.pid = pid;
    this.s3AccessKey = s3AccessKey;
    this.s3SecretKey = s3SecretKey;
    this.onFinishGlob = onFinishGlob;
    this.s3StorageClass = s3StorageClass;
    this.finishedAfterMillisWithoutNewFile = finishedAfterMillisWithoutNewFile;
    this.applyStorageClassIfOverBytes = applyStorageClassIfOverBytes;
    this.uploadImmediately = uploadImmediately;
    this.checkSubdirectories = checkSubdirectories.or(false);
    this.uploaderType = uploaderType.or(SingularityUploaderType.S3);
    this.gcsCredentials = gcsCredentials != null ? gcsCredentials : Collections.emptyMap();
    this.gcsStorageClass = gcsStorageClass;
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
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    S3UploadMetadata other = (S3UploadMetadata) obj;
    if (directory == null) {
      if (other.directory != null) {
        return false;
      }
    } else if (!directory.equals(other.directory)) {
      return false;
    }
    if (fileGlob == null) {
      if (other.fileGlob != null) {
        return false;
      }
    } else if (!fileGlob.equals(other.fileGlob)) {
      return false;
    }
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

  public Optional<Integer> getPid() {
    return pid;
  }

  public Optional<String> getS3AccessKey() {
    return s3AccessKey;
  }

  public Optional<String> getS3SecretKey() {
    return s3SecretKey;
  }

  public Optional<String> getOnFinishGlob() {
    return onFinishGlob;
  }

  public Optional<Long> getFinishedAfterMillisWithoutNewFile() {
    return finishedAfterMillisWithoutNewFile;
  }

  public Optional<String> getS3StorageClass() {
    return s3StorageClass;
  }

  public Optional<String> getGcsStorageClass() {
    return gcsStorageClass;
  }

  public Optional<Long> getApplyStorageClassIfOverBytes() {
    return applyStorageClassIfOverBytes;
  }

  public Optional<Boolean> getUploadImmediately() {
    return uploadImmediately;
  }

  public boolean isCheckSubdirectories() {
    return checkSubdirectories;
  }

  public SingularityUploaderType getUploaderType() {
    return uploaderType;
  }

  public Map<String, Object> getGcsCredentials() {
    return gcsCredentials;
  }

  @Override
  public String toString() {
    return "S3UploadMetadata{" +
        "directory='" + directory + '\'' +
        ", fileGlob='" + fileGlob + '\'' +
        ", s3Bucket='" + s3Bucket + '\'' +
        ", s3KeyFormat='" + s3KeyFormat + '\'' +
        ", finished=" + finished +
        ", onFinishGlob=" + onFinishGlob +
        ", pid=" + pid +
        ", s3AccessKey=" + s3AccessKey +
        ", s3SecretKey=" + s3SecretKey +
        ", finishedAfterMillisWithoutNewFile=" + finishedAfterMillisWithoutNewFile +
        ", s3StorageClass=" + s3StorageClass +
        ", applyStorageClassIfOverBytes=" + applyStorageClassIfOverBytes +
        ", uploadImmediately=" + uploadImmediately +
        ", checkSubdirectories=" + checkSubdirectories +
        ", uploaderType=" + uploaderType +
        ", gcsStorageClass=" + gcsStorageClass +
        '}';
  }
}
