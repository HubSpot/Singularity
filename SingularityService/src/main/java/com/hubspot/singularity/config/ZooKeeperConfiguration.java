package com.hubspot.singularity.config;

import javax.validation.constraints.NotNull;

public class ZooKeeperConfiguration {

  @NotNull
  private String quorum;
  @NotNull
  private int sessionTimeoutMillis = 600_000;
  @NotNull
  private int connectTimeoutMillis = 60_000;
  @NotNull
  private int retryBaseSleepTimeMilliseconds = 1_000;
  @NotNull
  private int retryMaxTries = 3;
  @NotNull
  private String zkNamespace;

  public String getQuorum() {
    return quorum;
  }

  public void setQuorum(String quorum) {
    this.quorum = quorum;
  }

  public int getSessionTimeoutMillis() {
    return sessionTimeoutMillis;
  }

  public void setSessionTimeoutMillis(int sessionTimeoutMillis) {
    this.sessionTimeoutMillis = sessionTimeoutMillis;
  }

  public int getConnectTimeoutMillis() {
    return connectTimeoutMillis;
  }

  public void setConnectTimeoutMillis(int connectTimeoutMillis) {
    this.connectTimeoutMillis = connectTimeoutMillis;
  }

  public int getRetryBaseSleepTimeMilliseconds() {
    return retryBaseSleepTimeMilliseconds;
  }

  public void setRetryBaseSleepTimeMilliseconds(int retryBaseSleepTimeMilliseconds) {
    this.retryBaseSleepTimeMilliseconds = retryBaseSleepTimeMilliseconds;
  }

  public int getRetryMaxTries() {
    return retryMaxTries;
  }

  public void setRetryMaxTries(int retryMaxTries) {
    this.retryMaxTries = retryMaxTries;
  }

  public String getZkNamespace() {
    return zkNamespace;
  }

  public void setZkNamespace(String zkNamespace) {
    this.zkNamespace = zkNamespace;
  }

}
