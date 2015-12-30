package com.hubspot.singularity.oomkiller.config;

import javax.validation.constraints.Min;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.runner.base.configuration.BaseRunnerConfiguration;
import com.hubspot.singularity.runner.base.configuration.Configuration;

@Configuration(filename = "/etc/singularity.oomkiller.yaml", consolidatedField = "oomkiller")
public class SingularityOOMKillerConfiguration extends BaseRunnerConfiguration {
  @Min(1)
  @JsonProperty
  private double requestKillThresholdRatio = 1.0;

  @Min(1)
  @JsonProperty
  private double killProcessDirectlyThresholdRatio = 1.2;

  @JsonProperty
  @Min(1)
  private long checkForOOMEveryMillis = 100;

  @NotEmpty
  @JsonProperty
  private String slaveHostname = "localhost";

  @NotEmpty
  @JsonProperty
  private String cgroupProcsPathFormat = "/cgroup/cpu/mesos/%s/cgroup.procs";

  public SingularityOOMKillerConfiguration() {
    super(Optional.of("singularity-oomkiller.log"));
  }

  public double getRequestKillThresholdRatio() {
    return requestKillThresholdRatio;
  }

  public void setRequestKillThresholdRatio(double requestKillThresholdRatio) {
    this.requestKillThresholdRatio = requestKillThresholdRatio;
  }

  public double getKillProcessDirectlyThresholdRatio() {
    return killProcessDirectlyThresholdRatio;
  }

  public void setKillProcessDirectlyThresholdRatio(double killProcessDirectlyThresholdRatio) {
    this.killProcessDirectlyThresholdRatio = killProcessDirectlyThresholdRatio;
  }

  public long getCheckForOOMEveryMillis() {
    return checkForOOMEveryMillis;
  }

  public void setCheckForOOMEveryMillis(long checkForOOMEveryMillis) {
    this.checkForOOMEveryMillis = checkForOOMEveryMillis;
  }

  public String getSlaveHostname() {
    return slaveHostname;
  }

  public void setSlaveHostname(String slaveHostname) {
    this.slaveHostname = slaveHostname;
  }

  public String getCgroupProcsPathFormat() {
    return cgroupProcsPathFormat;
  }

  public void setCgroupProcsPathFormat(String cgroupProcsPathFormat) {
    this.cgroupProcsPathFormat = cgroupProcsPathFormat;
  }

  @Override
  public String toString() {
    return "SingularityOOMKillerConfiguration[" +
        "requestKillThresholdRatio=" + requestKillThresholdRatio +
        ", killProcessDirectlyThresholdRatio=" + killProcessDirectlyThresholdRatio +
        ", checkForOOMEveryMillis=" + checkForOOMEveryMillis +
        ", slaveHostname='" + slaveHostname + '\'' +
        ", cgroupProcsPathFormat='" + cgroupProcsPathFormat + '\'' +
        ']';
  }
}
