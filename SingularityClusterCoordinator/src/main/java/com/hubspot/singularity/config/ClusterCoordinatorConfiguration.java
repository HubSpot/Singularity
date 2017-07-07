package com.hubspot.singularity.config;

import java.util.List;

import javax.validation.Valid;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import io.dropwizard.Configuration;

public class ClusterCoordinatorConfiguration extends Configuration {
  /*
   * List of possible Singularity clusters. The first in the list will be considered the default
   */
  @NotEmpty
  private List<DataCenter> dataCenters;

  @JsonProperty("ui")
  @Valid
  private UIConfiguration uiConfiguration = new UIConfiguration();

  // Settings to inform the ui
  private Integer defaultMemory;
  private Integer defaultCpus;
  private Integer slaveHttpPort;
  private Optional<Integer> slaveHttpsPort;
  private int bounceExpirationMinutes;
  private long healthcheckIntervalSeconds;
  private long healthcheckTimeoutSeconds;
  private Optional<Integer> healthcheckMaxRetries;
  private int startupTimeoutSeconds;
  private boolean loadBalancingEnabled;
  private Optional<String> commonHostnameSuffixToOmit;
  private Integer warnIfScheduledJobIsRunningPastNextRunPct;

  public List<DataCenter> getDataCenters() {
    return dataCenters;
  }

  public void setDataCenters(List<DataCenter> dataCenters) {
    this.dataCenters = dataCenters;
  }

  public UIConfiguration getUiConfiguration() {
    return uiConfiguration;
  }

  public void setUiConfiguration(UIConfiguration uiConfiguration) {
    this.uiConfiguration = uiConfiguration;
  }

  public Integer getDefaultMemory() {
    return defaultMemory;
  }

  public void setDefaultMemory(Integer defaultMemory) {
    this.defaultMemory = defaultMemory;
  }

  public Integer getDefaultCpus() {
    return defaultCpus;
  }

  public void setDefaultCpus(Integer defaultCpus) {
    this.defaultCpus = defaultCpus;
  }

  public Integer getSlaveHttpPort() {
    return slaveHttpPort;
  }

  public void setSlaveHttpPort(Integer slaveHttpPort) {
    this.slaveHttpPort = slaveHttpPort;
  }

  public Optional<Integer> getSlaveHttpsPort() {
    return slaveHttpsPort;
  }

  public void setSlaveHttpsPort(Optional<Integer> slaveHttpsPort) {
    this.slaveHttpsPort = slaveHttpsPort;
  }

  public int getBounceExpirationMinutes() {
    return bounceExpirationMinutes;
  }

  public void setBounceExpirationMinutes(int bounceExpirationMinutes) {
    this.bounceExpirationMinutes = bounceExpirationMinutes;
  }

  public long getHealthcheckIntervalSeconds() {
    return healthcheckIntervalSeconds;
  }

  public void setHealthcheckIntervalSeconds(long healthcheckIntervalSeconds) {
    this.healthcheckIntervalSeconds = healthcheckIntervalSeconds;
  }

  public long getHealthcheckTimeoutSeconds() {
    return healthcheckTimeoutSeconds;
  }

  public void setHealthcheckTimeoutSeconds(long healthcheckTimeoutSeconds) {
    this.healthcheckTimeoutSeconds = healthcheckTimeoutSeconds;
  }

  public Optional<Integer> getHealthcheckMaxRetries() {
    return healthcheckMaxRetries;
  }

  public void setHealthcheckMaxRetries(Optional<Integer> healthcheckMaxRetries) {
    this.healthcheckMaxRetries = healthcheckMaxRetries;
  }

  public int getStartupTimeoutSeconds() {
    return startupTimeoutSeconds;
  }

  public void setStartupTimeoutSeconds(int startupTimeoutSeconds) {
    this.startupTimeoutSeconds = startupTimeoutSeconds;
  }

  public boolean isLoadBalancingEnabled() {
    return loadBalancingEnabled;
  }

  public void setLoadBalancingEnabled(boolean loadBalancingEnabled) {
    this.loadBalancingEnabled = loadBalancingEnabled;
  }

  public Optional<String> getCommonHostnameSuffixToOmit() {
    return commonHostnameSuffixToOmit;
  }

  public void setCommonHostnameSuffixToOmit(Optional<String> commonHostnameSuffixToOmit) {
    this.commonHostnameSuffixToOmit = commonHostnameSuffixToOmit;
  }

  public Integer getWarnIfScheduledJobIsRunningPastNextRunPct() {
    return warnIfScheduledJobIsRunningPastNextRunPct;
  }

  public void setWarnIfScheduledJobIsRunningPastNextRunPct(Integer warnIfScheduledJobIsRunningPastNextRunPct) {
    this.warnIfScheduledJobIsRunningPastNextRunPct = warnIfScheduledJobIsRunningPastNextRunPct;
  }
}
