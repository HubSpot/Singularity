package com.hubspot.singularity.oomkiller.config;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class SingularityOOMKillerConfiguration {

  private final double requestKillThresholdRatio;
  private final double killProcessDirectlyThresholdRatio;

  private final long checkForOOMEveryMillis;
  private final String slaveHostname;

  private final String cgroupProcsPathFormat;

  @Inject
  public SingularityOOMKillerConfiguration(
      @Named(SingularityOOMKillerConfigurationLoader.CHECK_FOR_OOM_EVERY_MILLIS) String checkForOOMEveryMillis,
      @Named(SingularityOOMKillerConfigurationLoader.REQUEST_KILL_THRESHOLD_RATIO) String requestKillThresholdRatio,
      @Named(SingularityOOMKillerConfigurationLoader.KILL_PROCESS_DIRECTLY_THRESHOLD_RATIO) String killProcessDirectlyThresholdRatio,
      @Named(SingularityOOMKillerConfigurationLoader.SLAVE_HOSTNAME) String slaveHostname,
      @Named(SingularityOOMKillerConfigurationLoader.CGROUP_PROCS_PATH_FORMAT) String cgroupsProcPathFormat
      ) {
    this.checkForOOMEveryMillis = Long.parseLong(checkForOOMEveryMillis);
    this.requestKillThresholdRatio = Double.parseDouble(requestKillThresholdRatio);
    this.killProcessDirectlyThresholdRatio = Double.parseDouble(requestKillThresholdRatio);
    this.slaveHostname = slaveHostname;
    this.cgroupProcsPathFormat = cgroupsProcPathFormat;
  }

  public String getCgroupProcsPathFormat() {
    return cgroupProcsPathFormat;
  }

  public double getRequestKillThresholdRatio() {
    return requestKillThresholdRatio;
  }

  public String getSlaveHostname() {
    return slaveHostname;
  }

  public double getKillProcessDirectlyThresholdRatio() {
    return killProcessDirectlyThresholdRatio;
  }

  public long getCheckForOOMEveryMillis() {
    return checkForOOMEveryMillis;
  }

  @Override
  public String toString() {
    return "SingularityOOMKillerConfiguration [requestKillThresholdRatio=" + requestKillThresholdRatio + ", killProcessDirectlyThresholdRatio=" + killProcessDirectlyThresholdRatio + ", checkForOOMEveryMillis=" + checkForOOMEveryMillis
        + ", slaveHostname=" + slaveHostname + ", cgroupProcsPathFormat=" + cgroupProcsPathFormat + "]";
  }

}
