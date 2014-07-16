package com.hubspot.singularity.oomkiller.config;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class SingularityOOMKillerConfiguration {

  private final double requestKillThresholdRatio;
  private final double killProcessDirectlyThresholdRatio;

  private final long checkForOOMEveryMillis;
  
  @Inject
  public SingularityOOMKillerConfiguration(
      @Named(SingularityOOMKillerConfigurationLoader.CHECK_FOR_OOM_EVERY_MILLIS) String checkForOOMEveryMillis, 
      @Named(SingularityOOMKillerConfigurationLoader.REQUEST_KILL_THRESHOLD_RATIO) String requestKillThresholdRatio, 
      @Named(SingularityOOMKillerConfigurationLoader.KILL_PROCESS_DIRECTLY_THRESHOLD_RATIO) String killProcessDirectlyThresholdRatio
      ) {
    this.checkForOOMEveryMillis = Long.parseLong(checkForOOMEveryMillis);
    this.requestKillThresholdRatio = Double.parseDouble(requestKillThresholdRatio);
    this.killProcessDirectlyThresholdRatio = Double.parseDouble(requestKillThresholdRatio);
  }

  public double getRequestKillThresholdRatio() {
    return requestKillThresholdRatio;
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
        + "]";
  }
  
}
