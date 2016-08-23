package com.hubspot.singularity.config;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.hubspot.singularity.SingularityDisabledActionType;

public class DisasterDetectionConfiguration {

  @JsonProperty
  private boolean enabled = false;

  @JsonProperty
  private long runEveryMillis = TimeUnit.SECONDS.toMillis(30);

  @JsonProperty
  private long considerOverdueAfterMillis = TimeUnit.MINUTES.toMillis(1);

  @JsonProperty
  @NotNull
  private List<SingularityDisabledActionType> disableActionsOnDisaster = ImmutableList.of(SingularityDisabledActionType.BOUNCE, SingularityDisabledActionType.DEPLOY, SingularityDisabledActionType.TASK_RECONCILIATION);

  @JsonProperty
  private boolean checkOverdueTasks = true;

  @JsonProperty
  private long criticalAvgTaskLagMillis = 300000L;

  @JsonProperty
  private double criticalOverdueTaskPortion = 0.2;

  @JsonProperty
  private boolean requireAllConditionsForOverdueTaskDisaster = true;

  @JsonProperty
  private boolean checkLostSlaves = true;

  @JsonProperty
  private double criticalLostSlavePortion = 0.2;

  @JsonProperty
  private long checkLostSlavesInLastMillis = 60000;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public long getRunEveryMillis() {
    return runEveryMillis;
  }

  public void setRunEveryMillis(long runEveryMillis) {
    this.runEveryMillis = runEveryMillis;
  }

  public long getConsiderOverdueAfterMillis() {
    return considerOverdueAfterMillis;
  }

  public void setConsiderOverdueAfterMillis(long considerOverdueAfterMillis) {
    this.considerOverdueAfterMillis = considerOverdueAfterMillis;
  }

  public List<SingularityDisabledActionType> getDisableActionsOnDisaster() {
    return disableActionsOnDisaster;
  }

  public void setDisableActionsOnDisaster(List<SingularityDisabledActionType> disableActionsOnDisaster) {
    this.disableActionsOnDisaster = disableActionsOnDisaster;
  }

  public boolean isCheckOverdueTasks() {
    return checkOverdueTasks;
  }

  public void setCheckOverdueTasks(boolean checkOverdueTasks) {
    this.checkOverdueTasks = checkOverdueTasks;
  }

  public long getCriticalAvgTaskLagMillis() {
    return criticalAvgTaskLagMillis;
  }

  public void setCriticalAvgTaskLagMillis(long criticalAvgTaskLagMillis) {
    this.criticalAvgTaskLagMillis = criticalAvgTaskLagMillis;
  }

  public double getCriticalOverdueTaskPortion() {
    return criticalOverdueTaskPortion;
  }

  public void setCriticalOverdueTaskPortion(double criticalOverdueTaskPortion) {
    this.criticalOverdueTaskPortion = criticalOverdueTaskPortion;
  }

  public boolean isRequireAllConditionsForOverdueTaskDisaster() {
    return requireAllConditionsForOverdueTaskDisaster;
  }

  public void setRequireAllConditionsForOverdueTaskDisaster(boolean requireAllConditionsForOverdueTaskDisaster) {
    this.requireAllConditionsForOverdueTaskDisaster = requireAllConditionsForOverdueTaskDisaster;
  }

  public boolean isCheckLostSlaves() {
    return checkLostSlaves;
  }

  public void setCheckLostSlaves(boolean checkLostSlaves) {
    this.checkLostSlaves = checkLostSlaves;
  }

  public double getCriticalLostSlavePortion() {
    return criticalLostSlavePortion;
  }

  public void setCriticalLostSlavePortion(double criticalLostSlavePortion) {
    this.criticalLostSlavePortion = criticalLostSlavePortion;
  }

  public long getCheckLostSlavesInLastMillis() {
    return checkLostSlavesInLastMillis;
  }

  public void setCheckLostSlavesInLastMillis(long checkLostSlavesInLastMillis) {
    this.checkLostSlavesInLastMillis = checkLostSlavesInLastMillis;
  }
}
