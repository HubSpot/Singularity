package com.hubspot.singularity.helpers;

import com.amazonaws.services.s3.AmazonS3;

public class SingularityS3Service {
  private final String group;
  private final String bucket;
  private final AmazonS3 s3Client;

  public SingularityS3Service(String group, String bucket, AmazonS3 s3Client) {
    this.group = group;
    this.bucket = bucket;
    this.s3Client = s3Client;
  }

  public String getGroup() {
    return group;
  }

  public String getBucket() {
    return bucket;
  }

  public AmazonS3 getS3Client() {
    return s3Client;
  }

  @Override
  public String toString() {
    return "SingularityS3Service{" +
        "group='" + group + '\'' +
        ", bucket='" + bucket + '\'' +
        ", s3Client=" + s3Client +
        '}';
  }
}
