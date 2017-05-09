package com.hubspot.singularity.config;

import java.util.Objects;

import javax.validation.constraints.NotNull;

public class S3GroupConfiguration {
  @NotNull
  private String s3Bucket;

  @NotNull
  private String s3AccessKey;

  @NotNull
  private String s3SecretKey;

  public String getS3Bucket() {
    return s3Bucket;
  }

  public void setS3Bucket(String s3Bucket) {
    this.s3Bucket = s3Bucket;
  }

  public String getS3AccessKey() {
    return s3AccessKey;
  }

  public void setS3AccessKey(String s3AccessKey) {
    this.s3AccessKey = s3AccessKey;
  }

  public String getS3SecretKey() {
    return s3SecretKey;
  }

  public void setS3SecretKey(String s3SecretKey) {
    this.s3SecretKey = s3SecretKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    S3GroupConfiguration that = (S3GroupConfiguration) o;
    return Objects.equals(s3Bucket, that.s3Bucket) &&
            Objects.equals(s3AccessKey, that.s3AccessKey) &&
            Objects.equals(s3SecretKey, that.s3SecretKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(s3Bucket, s3AccessKey, s3SecretKey);
  }
}
