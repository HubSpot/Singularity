package com.hubspot.singularity.config;

import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

public class AthenaConfig {
  @NotNull
  private String s3AccessKey;

  @NotNull
  private String s3SecretKey;

  @NotNull
  private String athenaUrl = "https://athena.us-east-1.amazonaws.com:443";

  @NotNull
  private String databaseName = "singularity";

  @NotNull
  private String defaultSchema = "default";

  @NotNull
  private String s3StagingBucket;

  @NotNull
  private String s3StagingPrefix = "";

  /**
   * Links to logs will expire after given number of milliseconds.
   * A new link is generated for every /logs API call.
   */
  @NotNull
  private long expireS3LinksAfterMillis = TimeUnit.DAYS.toMillis(1);

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

  public String getAthenaUrl() {
    return athenaUrl;
  }

  public void setAthenaUrl(String athenaUrl) {
    this.athenaUrl = athenaUrl;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  public void setDatabaseName(String databaseName) {
    this.databaseName = databaseName;
  }

  public String getDefaultSchema() {
    return defaultSchema;
  }

  public void setDefaultSchema(String defaultSchema) {
    this.defaultSchema = defaultSchema;
  }

  public String getS3StagingBucket() {
    return s3StagingBucket;
  }

  public void setS3StagingBucket(String s3StagingBucket) {
    this.s3StagingBucket = s3StagingBucket;
  }

  public String getS3StagingPrefix() {
    return s3StagingPrefix;
  }

  public void setS3StagingPrefix(String s3StagingPrefix) {
    this.s3StagingPrefix = s3StagingPrefix;
  }

  public long getExpireS3LinksAfterMillis() {
    return expireS3LinksAfterMillis;
  }

  public void setExpireS3LinksAfterMillis(long expireS3LinksAfterMillis) {
    this.expireS3LinksAfterMillis = expireS3LinksAfterMillis;
  }
}
