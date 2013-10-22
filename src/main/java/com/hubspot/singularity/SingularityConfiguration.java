package com.hubspot.singularity;

import com.codahale.dropwizard.Configuration;

public class SingularityConfiguration extends Configuration {

  private String master;
  private String quorum;
  private int sessionTimeoutMillis;
  private int connectTimeoutMillis;
  private int retryBaseSleepTimeMilliseconds;
  private int retryMaxTries;
  private String zkNamespace;
  
  public String getZkNamespace() {
    return zkNamespace;
  }

  public void setZkNamespace(String zkNamespace) {
    this.zkNamespace = zkNamespace;
  }

  public void setMaster(String master) {
    this.master = master;
  }

  public String getMaster() {
    return master;
  }
  
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
  
}
