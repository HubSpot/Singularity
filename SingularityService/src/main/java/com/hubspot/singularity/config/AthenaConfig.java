package com.hubspot.singularity.config;

import javax.validation.constraints.NotNull;

public class AthenaConfig {
  @NotNull
  private String s3AccessKey;

  @NotNull
  private String s3SecretKey;

  @NotNull
  private String athenaUrl = "jdbc:awsathena://athena.us-east-1.amazonaws.com:443";

  @NotNull
  private String database = "default";

  @NotNull
  private String s3StagingBucket;

  @NotNull
  private String s3StagingPrefix = "";

  public String getS3AccessKey() {
    return s3AccessKey;
  }

  public AthenaConfig setS3AccessKey(String s3AccessKey) {
    this.s3AccessKey = s3AccessKey;
    return this;
  }

  public String getS3SecretKey() {
    return s3SecretKey;
  }

  public AthenaConfig setS3SecretKey(String s3SecretKey) {
    this.s3SecretKey = s3SecretKey;
    return this;
  }

  public String getAthenaUrl() {
    return athenaUrl;
  }

  public AthenaConfig setAthenaUrl(String athenaUrl) {
    this.athenaUrl = athenaUrl;
    return this;
  }

  public String getS3StagingBucket() {
    return s3StagingBucket;
  }

  public AthenaConfig setS3StagingBucket(String s3StagingBucket) {
    this.s3StagingBucket = s3StagingBucket;
    return this;
  }

  public String getS3StagingPrefix() {
    return s3StagingPrefix;
  }

  public AthenaConfig setS3StagingPrefix(String s3StagingPrefix) {
    this.s3StagingPrefix = s3StagingPrefix;
    return this;
  }

  public String getDatabase() {
    return database;
  }

  public AthenaConfig setDatabase(String database) {
    this.database = database;
    return this;
  }
}
