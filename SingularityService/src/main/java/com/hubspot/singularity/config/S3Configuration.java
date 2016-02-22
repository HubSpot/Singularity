package com.hubspot.singularity.config;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

public class S3Configuration {

  @NotNull
  private int maxS3Threads = 3;

  @NotNull
  private int waitForS3ListSeconds = 5;

  @NotNull
  private int waitForS3LinksSeconds = 1;

  @NotNull
  private long missingTaskDefaultS3SearchPeriodMillis = TimeUnit.DAYS.toMillis(3);

  /**
   * Links to logs will expire after given number of milliseconds.
   * A new link is generated for every /logs API call.
   */
  @NotNull
  private long expireS3LinksAfterMillis = TimeUnit.DAYS.toMillis(1);

  @NotNull
  private String s3Bucket;

  @NotNull
  private Map<String, S3GroupOverrideConfiguration> groupOverrides = new HashMap<>();

  /**
   * S3 Key format for finding logs. Should be the same as
   * configuration set for SingularityS3Uploader
   * (e.g. '%requestId/%Y/%m/%taskId_%index-%s%fileext')
   */
  @NotNull
  private String s3KeyFormat;

  @NotNull
  private String s3AccessKey;

  @NotNull
  private String s3SecretKey;

  public int getMaxS3Threads() {
    return maxS3Threads;
  }

  public void setMaxS3Threads(int maxS3Threads) {
    this.maxS3Threads = maxS3Threads;
  }

  public int getWaitForS3ListSeconds() {
    return waitForS3ListSeconds;
  }

  public void setWaitForS3ListSeconds(int waitForS3ListSeconds) {
    this.waitForS3ListSeconds = waitForS3ListSeconds;
  }

  public int getWaitForS3LinksSeconds() {
    return waitForS3LinksSeconds;
  }

  public long getMissingTaskDefaultS3SearchPeriodMillis() {
    return missingTaskDefaultS3SearchPeriodMillis;
  }

  public void setMissingTaskDefaultS3SearchPeriodMillis(long missingTaskDefaultS3SearchPeriodMillis) {
    this.missingTaskDefaultS3SearchPeriodMillis = missingTaskDefaultS3SearchPeriodMillis;
  }

  public void setWaitForS3LinksSeconds(int waitForS3LinksSeconds) {
    this.waitForS3LinksSeconds = waitForS3LinksSeconds;
  }

  public long getExpireS3LinksAfterMillis() {
    return expireS3LinksAfterMillis;
  }

  public void setExpireS3LinksAfterMillis(long expireS3LinksAfterMillis) {
    this.expireS3LinksAfterMillis = expireS3LinksAfterMillis;
  }

  public String getS3Bucket() {
    return s3Bucket;
  }

  public void setS3Bucket(String s3Bucket) {
    this.s3Bucket = s3Bucket;
  }

  public String getS3KeyFormat() {
    return s3KeyFormat;
  }

  public void setS3KeyFormat(String s3KeyFormat) {
    this.s3KeyFormat = s3KeyFormat;
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

  public Map<String, S3GroupOverrideConfiguration> getGroupOverrides() {
    return groupOverrides;
  }

  public void setGroupOverrides(Map<String, S3GroupOverrideConfiguration> groupOverrides) {
    this.groupOverrides = groupOverrides;
  }
}
