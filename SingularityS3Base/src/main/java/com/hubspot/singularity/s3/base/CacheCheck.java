package com.hubspot.singularity.s3.base;

public class CacheCheck {
  private final CacheCheckResult cacheCheckResult;
  private final String message;

  public CacheCheck(CacheCheckResult cacheCheckResult, String message) {
    this.cacheCheckResult = cacheCheckResult;
    this.message = message;
  }

  public CacheCheckResult getCacheCheckResult() {
    return cacheCheckResult;
  }

  public String getMessage() {
    return message;
  }
}
