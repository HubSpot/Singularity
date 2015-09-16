package com.hubspot.singularity.oomkiller.config;

import java.util.Properties;

import javax.validation.constraints.Min;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.runner.base.configuration.BaseRunnerConfiguration;
import com.hubspot.singularity.runner.base.configuration.Configuration;

@Configuration(filename = "/etc/singularity.oomkiller.yaml", consolidatedField = "oomkiller")
public class SingularityOOMKillerConfiguration extends BaseRunnerConfiguration {
  public static final String REQUEST_KILL_THRESHOLD_RATIO = "oomkiller.request.kill.threshold.ratio";
  public static final String KILL_PROCESS_DIRECTLY_THRESHOLD_RATIO = "oomkiller.kill.process.directly.threshold.ratio";
  public static final String CHECK_FOR_OOM_EVERY_MILLIS = "oomkiller.check.for.oom.every.millis";
  public static final String SLAVE_HOSTNAME = "oomkiller.slave.hostname";
  public static final String CGROUP_PROCS_PATH_FORMAT = "oomkiller.cgroups.procs.path.format";

  @JsonProperty
  private double requestKillThresholdRatio = 1.0;

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

  @Override
  public void updateFromProperties(Properties properties) {
    if (properties.containsKey(REQUEST_KILL_THRESHOLD_RATIO)) {
      setRequestKillThresholdRatio(Double.parseDouble(properties.getProperty(REQUEST_KILL_THRESHOLD_RATIO)));
    }

    if (properties.containsKey(KILL_PROCESS_DIRECTLY_THRESHOLD_RATIO)) {
      setKillProcessDirectlyThresholdRatio(Double.parseDouble(properties.getProperty(KILL_PROCESS_DIRECTLY_THRESHOLD_RATIO)));
    }

    if (properties.containsKey(CHECK_FOR_OOM_EVERY_MILLIS)) {
      setCheckForOOMEveryMillis(Long.parseLong(properties.getProperty(CHECK_FOR_OOM_EVERY_MILLIS)));
    }

    if (properties.containsKey(SLAVE_HOSTNAME)) {
      setSlaveHostname(properties.getProperty(SLAVE_HOSTNAME));
    }

    if (properties.containsKey(CGROUP_PROCS_PATH_FORMAT)) {
      setCgroupProcsPathFormat(properties.getProperty(CGROUP_PROCS_PATH_FORMAT));
    }
  }
}
