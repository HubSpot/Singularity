package com.hubspot.singularity.config;

import com.google.common.base.Optional;

public class IndexViewConfiguration {
  private final UIConfiguration uiConfiguration;
  private final Integer defaultMemory;
  private final Integer defaultCpus;
  private final Integer slaveHttpPort;
  private final Optional<Integer> slaveHttpsPort;
  private final int bounceExpirationMinutes;
  private final long healthcheckIntervalSeconds;
  private final long healthcheckTimeoutSeconds;
  private final Optional<Integer> healthcheckMaxRetries;
  private final int startupTimeoutSeconds;
  private final boolean loadBalancingEnabled;
  private final Optional<String> commonHostnameSuffixToOmit;
  private final Integer warnIfScheduledJobIsRunningPastNextRunPct;
  private final boolean generateAuthHeader;

  public IndexViewConfiguration(UIConfiguration uiConfiguration,
                                Integer defaultMemory,
                                Integer defaultCpus,
                                Integer slaveHttpPort,
                                Optional<Integer> slaveHttpsPort,
                                int bounceExpirationMinutes,
                                long healthcheckIntervalSeconds,
                                long healthcheckTimeoutSeconds,
                                Optional<Integer> healthcheckMaxRetries,
                                int startupTimeoutSeconds,
                                boolean loadBalancingEnabled,
                                Optional<String> commonHostnameSuffixToOmit, Integer warnIfScheduledJobIsRunningPastNextRunPct,
                                boolean generateAuthHeader) {
    this.uiConfiguration = uiConfiguration;
    this.defaultMemory = defaultMemory;
    this.defaultCpus = defaultCpus;
    this.slaveHttpPort = slaveHttpPort;
    this.slaveHttpsPort = slaveHttpsPort;
    this.bounceExpirationMinutes = bounceExpirationMinutes;
    this.healthcheckIntervalSeconds = healthcheckIntervalSeconds;
    this.healthcheckTimeoutSeconds = healthcheckTimeoutSeconds;
    this.healthcheckMaxRetries = healthcheckMaxRetries;
    this.startupTimeoutSeconds = startupTimeoutSeconds;
    this.loadBalancingEnabled = loadBalancingEnabled;
    this.commonHostnameSuffixToOmit = commonHostnameSuffixToOmit;
    this.warnIfScheduledJobIsRunningPastNextRunPct = warnIfScheduledJobIsRunningPastNextRunPct;
    this.generateAuthHeader = generateAuthHeader;
  }

  public UIConfiguration getUiConfiguration() {
    return uiConfiguration;
  }

  public Integer getDefaultMemory() {
    return defaultMemory;
  }

  public Integer getDefaultCpus() {
    return defaultCpus;
  }

  public Integer getSlaveHttpPort() {
    return slaveHttpPort;
  }

  public Optional<Integer> getSlaveHttpsPort() {
    return slaveHttpsPort;
  }

  public int getBounceExpirationMinutes() {
    return bounceExpirationMinutes;
  }

  public long getHealthcheckIntervalSeconds() {
    return healthcheckIntervalSeconds;
  }

  public long getHealthcheckTimeoutSeconds() {
    return healthcheckTimeoutSeconds;
  }

  public Optional<Integer> getHealthcheckMaxRetries() {
    return healthcheckMaxRetries;
  }

  public int getStartupTimeoutSeconds() {
    return startupTimeoutSeconds;
  }

  public boolean isLoadBalancingEnabled() {
    return loadBalancingEnabled;
  }

  public Optional<String> getCommonHostnameSuffixToOmit() {
    return commonHostnameSuffixToOmit;
  }

  public Integer getWarnIfScheduledJobIsRunningPastNextRunPct() {
    return warnIfScheduledJobIsRunningPastNextRunPct;
  }

  public boolean isGenerateAuthHeader() {
    return generateAuthHeader;
  }
}
