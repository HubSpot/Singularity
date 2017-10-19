package com.hubspot.singularity.config;

import java.util.List;

import javax.validation.Valid;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityClientCredentials;

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

  private Optional<SingularityClientCredentials> defaultClientCredentials;

  private boolean errorOnDataCenterNotSpecified = false;

  // Settings to inform the ui
  private Integer defaultMemory = 64;
  private Integer defaultCpus = 1;
  private Integer slaveHttpPort = 5051;
  private Optional<Integer> slaveHttpsPort = Optional.absent();
  private int bounceExpirationMinutes = 60;
  private long healthcheckIntervalSeconds = 5;
  private long healthcheckTimeoutSeconds = 5;
  private Optional<Integer> healthcheckMaxRetries = Optional.absent();
  private int startupTimeoutSeconds = 45;
  private boolean loadBalancingEnabled = false;
  private Optional<String> commonHostnameSuffixToOmit = Optional.absent();
  private Integer warnIfScheduledJobIsRunningPastNextRunPct = 200;



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

  public Optional<SingularityClientCredentials> getDefaultClientCredentials() {
    return defaultClientCredentials;
  }

  public void setDefaultClientCredentials(Optional<SingularityClientCredentials> defaultClientCredentials) {
    this.defaultClientCredentials = defaultClientCredentials;
  }

  public boolean isErrorOnDataCenterNotSpecified() {
    return errorOnDataCenterNotSpecified;
  }

  public ClusterCoordinatorConfiguration setErrorOnDataCenterNotSpecified(boolean errorOnDataCenterNotSpecified) {
    this.errorOnDataCenterNotSpecified = errorOnDataCenterNotSpecified;
    return this;
  }
}
