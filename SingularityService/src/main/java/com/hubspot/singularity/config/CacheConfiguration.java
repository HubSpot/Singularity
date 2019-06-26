package com.hubspot.singularity.config;

import java.util.concurrent.TimeUnit;

public class CacheConfiguration {
  private int atomixStartTimeoutSeconds = 30;
  private int atomixPort = 5283;
  private int pendingTaskCacheSize = 10000;
  private int requestCacheSize = 15000;
  private int cleanupTasksCacheSize = 1000;
  private int historyUpdateCacheSize = 20000;
  private int slaveCacheSize = 1000;
  private int rackCacheSize = 100;

  private int taskCacheMaxSize = 5000;
  private int taskCacheInitialSize = 100;
  private long cacheTasksForMillis = TimeUnit.DAYS.toMillis(1);
  private int deployCacheMaxSize = 5000;
  private int deployCacheInitialSize = 100;
  private long cacheDeployssForMillis = TimeUnit.DAYS.toMillis(1);

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

  public int getTaskCacheMaxSize() {
    return taskCacheMaxSize;
  }

  public void setTaskCacheMaxSize(int taskCacheMaxSize) {
    this.taskCacheMaxSize = taskCacheMaxSize;
  }

  public int getTaskCacheInitialSize() {
    return taskCacheInitialSize;
  }

  public void setTaskCacheInitialSize(int taskCacheInitialSize) {
    this.taskCacheInitialSize = taskCacheInitialSize;
  }

  public long getCacheTasksForMillis() {
    return cacheTasksForMillis;
  }

  public void setCacheTasksForMillis(long cacheTasksForMillis) {
    this.cacheTasksForMillis = cacheTasksForMillis;
  }

  public int getDeployCacheMaxSize() {
    return deployCacheMaxSize;
  }

  public void setDeployCacheMaxSize(int deployCacheMaxSize) {
    this.deployCacheMaxSize = deployCacheMaxSize;
  }

  public int getDeployCacheInitialSize() {
    return deployCacheInitialSize;
  }

  public void setDeployCacheInitialSize(int deployCacheInitialSize) {
    this.deployCacheInitialSize = deployCacheInitialSize;
  }

  public long getCacheDeployssForMillis() {
    return cacheDeployssForMillis;
  }

  public void setCacheDeployssForMillis(long cacheDeployssForMillis) {
    this.cacheDeployssForMillis = cacheDeployssForMillis;
  }
}
