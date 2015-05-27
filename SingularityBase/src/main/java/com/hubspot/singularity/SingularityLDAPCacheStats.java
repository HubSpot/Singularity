package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.cache.CacheStats;

public class SingularityLDAPCacheStats {
  private final long hitCount;
  private final long missCount;
  private final long loadSuccessCount;
  private final long loadExceptionCount;
  private final long totalLoadTime;
  private final long evictionCount;

  public static SingularityLDAPCacheStats fromGuavaCacheStats(CacheStats cacheStats) {
    return new SingularityLDAPCacheStats(cacheStats.hitCount(), cacheStats.missCount(), cacheStats.loadSuccessCount(), cacheStats.loadExceptionCount(), cacheStats.totalLoadTime(), cacheStats.evictionCount());
  }

  @JsonCreator
  public SingularityLDAPCacheStats(@JsonProperty("hitCount") long hitCount, @JsonProperty("missCount") long missCount, @JsonProperty("loadSuccessCount") long loadSuccessCount, @JsonProperty("loadExceptionCount") long loadExceptionCount, @JsonProperty("totalLoadTime") long totalLoadTime, @JsonProperty("evictionCount") long evictionCount) {
    this.hitCount = hitCount;
    this.missCount = missCount;
    this.loadSuccessCount = loadSuccessCount;
    this.loadExceptionCount = loadExceptionCount;
    this.totalLoadTime = totalLoadTime;
    this.evictionCount = evictionCount;
  }

  public long getHitCount() {
    return hitCount;
  }

  public long getMissCount() {
    return missCount;
  }

  public long getLoadSuccessCount() {
    return loadSuccessCount;
  }

  public long getLoadExceptionCount() {
    return loadExceptionCount;
  }

  public long getTotalLoadTime() {
    return totalLoadTime;
  }

  public long getEvictionCount() {
    return evictionCount;
  }

  @Override
  public String toString() {
    return "SingularityLDAPCacheStats[" +
            "hitCount=" + hitCount +
            ", missCount=" + missCount +
            ", loadSuccessCount=" + loadSuccessCount +
            ", loadExceptionCount=" + loadExceptionCount +
            ", totalLoadTime=" + totalLoadTime +
            ", evictionCount=" + evictionCount +
            ']';
  }
}
