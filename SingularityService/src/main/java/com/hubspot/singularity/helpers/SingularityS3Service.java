package com.hubspot.singularity.helpers;

import org.jets3t.service.S3Service;

public class SingularityS3Service {
  private final String group;
  private final String bucket;
  private final S3Service s3Service;

  public SingularityS3Service(String group, String bucket, S3Service s3Service) {
    this.group = group;
    this.bucket = bucket;
    this.s3Service = s3Service;
  }

  public String getGroup() {
    return group;
  }

  public String getBucket() {
    return bucket;
  }

  public S3Service getS3Service() {
    return s3Service;
  }

  @Override
  public String toString() {
    return "SingularityS3Service{" +
        "group='" + group + '\'' +
        ", bucket='" + bucket + '\'' +
        ", s3Service=" + s3Service +
        '}';
  }
}
