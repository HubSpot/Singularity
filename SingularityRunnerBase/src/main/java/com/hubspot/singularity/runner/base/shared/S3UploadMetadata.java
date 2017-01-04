package com.hubspot.singularity.runner.base.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.hubspot.singularity.SingularityS3FormatHelper;

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
 * %group - request group name or 'default'
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
  private final String groupName;

  @JsonCreator
  public S3UploadMetadata(@JsonProperty("directory") String directory, @JsonProperty("fileGlob") String fileGlob, @JsonProperty("s3Bucket") String s3Bucket, @JsonProperty("s3KeyFormat") String s3KeyFormat,
      @JsonProperty("finished") boolean finished, @JsonProperty("onFinishGlob") Optional<String> onFinishGlob, @JsonProperty("pid") Optional<Integer> pid, @JsonProperty("s3AccessKey") Optional<String> s3AccessKey,
      @JsonProperty("s3SecretKey") Optional<String> s3SecretKey, @JsonProperty("finishedAfterMillisWithoutNewFile") Optional<Long> finishedAfterMillisWithoutNewFile,
      @JsonProperty("storageClass") Optional<String> s3StorageClass, @JsonProperty("applyStorageClassIfOverBytes") Optional<Long> applyStorageClassIfOverBytes, @JsonProperty("groupName") Optional<String> groupName) {
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
    this.groupName = groupName.or(SingularityS3FormatHelper.DEFAULT_GROUP_NAME);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    S3UploadMetadata that = (S3UploadMetadata) o;

    if (finished != that.finished) {
      return false;
    }
    if (directory != null ? !directory.equals(that.directory) : that.directory != null) {
      return false;
    }
    if (fileGlob != null ? !fileGlob.equals(that.fileGlob) : that.fileGlob != null) {
      return false;
    }
    if (s3Bucket != null ? !s3Bucket.equals(that.s3Bucket) : that.s3Bucket != null) {
      return false;
    }
    if (s3KeyFormat != null ? !s3KeyFormat.equals(that.s3KeyFormat) : that.s3KeyFormat != null) {
      return false;
    }
    if (onFinishGlob != null ? !onFinishGlob.equals(that.onFinishGlob) : that.onFinishGlob != null) {
      return false;
    }
    if (pid != null ? !pid.equals(that.pid) : that.pid != null) {
      return false;
    }
    if (s3AccessKey != null ? !s3AccessKey.equals(that.s3AccessKey) : that.s3AccessKey != null) {
      return false;
    }
    if (s3SecretKey != null ? !s3SecretKey.equals(that.s3SecretKey) : that.s3SecretKey != null) {
      return false;
    }
    if (finishedAfterMillisWithoutNewFile != null ? !finishedAfterMillisWithoutNewFile.equals(that.finishedAfterMillisWithoutNewFile) : that.finishedAfterMillisWithoutNewFile != null) {
      return false;
    }
    return groupName != null ? groupName.equals(that.groupName) : that.groupName == null;
  }

  @Override
  public int hashCode() {
    int result = directory != null ? directory.hashCode() : 0;
    result = 31 * result + (fileGlob != null ? fileGlob.hashCode() : 0);
    result = 31 * result + (s3Bucket != null ? s3Bucket.hashCode() : 0);
    result = 31 * result + (s3KeyFormat != null ? s3KeyFormat.hashCode() : 0);
    result = 31 * result + (finished ? 1 : 0);
    result = 31 * result + (onFinishGlob != null ? onFinishGlob.hashCode() : 0);
    result = 31 * result + (pid != null ? pid.hashCode() : 0);
    result = 31 * result + (s3AccessKey != null ? s3AccessKey.hashCode() : 0);
    result = 31 * result + (s3SecretKey != null ? s3SecretKey.hashCode() : 0);
    result = 31 * result + (finishedAfterMillisWithoutNewFile != null ? finishedAfterMillisWithoutNewFile.hashCode() : 0);
    result = 31 * result + (groupName != null ? groupName.hashCode() : 0);
    return result;
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

  public Optional<Long> getApplyStorageClassIfOverBytes() {
    return applyStorageClassIfOverBytes;
  }

  public String getGroupName() {
    return groupName;
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
        ", groupName='" + groupName + '\'' +
        '}';
  }
}
