package com.hubspot.singularity.helpers;

import java.util.Collections;
import java.util.List;

public class SingularityS3Services {
  private final boolean s3ConfigPresent;
  private final List<SingularityS3Service> s3Services;
  private final SingularityS3Service defaultS3Service;

  public SingularityS3Services() {
    this.s3ConfigPresent = false;
    this.s3Services = Collections.emptyList();
    this.defaultS3Service = null;
  }

  public SingularityS3Services(List<SingularityS3Service> s3Services, SingularityS3Service defaultS3Service) {
    this.s3ConfigPresent = true;
    this.s3Services = s3Services;
    this.defaultS3Service = defaultS3Service;
  }

  public boolean isS3ConfigPresent() {
    return s3ConfigPresent;
  }

  public List<SingularityS3Service> getS3Services() {
    return s3Services;
  }

  public SingularityS3Service getDefaultS3Service() {
    return defaultS3Service;
  }

  public SingularityS3Service getServiceByGroupAndBucketOrDefault(String group, String bucket) {
    for (SingularityS3Service s3Service : s3Services) {
      if (s3Service.getGroup().equals(group) && s3Service.getBucket().equals(bucket)) {
        return s3Service;
      }
    }
    return defaultS3Service;
  }
}
