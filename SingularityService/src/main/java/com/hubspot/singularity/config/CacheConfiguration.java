package com.hubspot.singularity.config;

public class CacheConfiguration {
  private int atomixStartTimeoutSeconds = 30;
  private int atomixPort = 5283;
  private int pendingTaskCacheSize = 10000;
  private int requestCacheSize = 15000;
  private int cleanupTasksCacheSize = 1000;
  private int historyUpdateCacheSize = 20000;
  private int slaveCacheSize = 1000;
  private int rackCacheSize = 100;

  public int getAtomixStartTimeoutSeconds() {
    return atomixStartTimeoutSeconds;
  }

  public void setAtomixStartTimeoutSeconds(int atomixStartTimeoutSeconds) {
    this.atomixStartTimeoutSeconds = atomixStartTimeoutSeconds;
  }

  public int getAtomixPort() {
    return atomixPort;
  }

  public void setAtomixPort(int atomixPort) {
    this.atomixPort = atomixPort;
  }

  public int getPendingTaskCacheSize() {
    return pendingTaskCacheSize;
  }

  public void setPendingTaskCacheSize(int pendingTaskCacheSize) {
    this.pendingTaskCacheSize = pendingTaskCacheSize;
  }

  public int getRequestCacheSize() {
    return requestCacheSize;
  }

  public void setRequestCacheSize(int requestCacheSize) {
    this.requestCacheSize = requestCacheSize;
  }

  public int getCleanupTasksCacheSize() {
    return cleanupTasksCacheSize;
  }

  public void setCleanupTasksCacheSize(int cleanupTasksCacheSize) {
    this.cleanupTasksCacheSize = cleanupTasksCacheSize;
  }

  public int getHistoryUpdateCacheSize() {
    return historyUpdateCacheSize;
  }

  public void setHistoryUpdateCacheSize(int historyUpdateCacheSize) {
    this.historyUpdateCacheSize = historyUpdateCacheSize;
  }

  public int getSlaveCacheSize() {
    return slaveCacheSize;
  }

  public void setSlaveCacheSize(int slaveCacheSize) {
    this.slaveCacheSize = slaveCacheSize;
  }

  public int getRackCacheSize() {
    return rackCacheSize;
  }

  public void setRackCacheSize(int rackCacheSize) {
    this.rackCacheSize = rackCacheSize;
  }
}
