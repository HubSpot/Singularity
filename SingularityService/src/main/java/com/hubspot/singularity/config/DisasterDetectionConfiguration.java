package com.hubspot.singularity.config;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

import org.apache.mesos.Protos.TaskStatus.Reason;

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
  private List<SingularityDisabledActionType> disableActionsOnDisaster = ImmutableList.of(
    SingularityDisabledActionType.BOUNCE, SingularityDisabledActionType.DEPLOY, SingularityDisabledActionType.TASK_RECONCILIATION);

  @JsonProperty
  private boolean checkLateTasks = false;

  @JsonProperty
  private long criticalAvgTaskLagMillis = 300000L;

  @JsonProperty
  private double criticalOverdueTaskPortion = 0.2;

  @JsonProperty
  private boolean checkLostSlaves = false;

  @JsonProperty
  private double criticalLostSlavePortion = 0.2;

  @JsonProperty
  private long checkLostSlavesInLastMillis = 60000;

  @JsonProperty
  private boolean checkLostTasks = false;

  @JsonProperty
  @NotNull
  private List<Reason> lostTaskReasons = ImmutableList.of(
    Reason.REASON_INVALID_OFFERS, Reason.REASON_SLAVE_UNKNOWN, Reason.REASON_SLAVE_REMOVED, Reason.REASON_SLAVE_RESTARTED, Reason.REASON_MASTER_DISCONNECTED);

  @JsonProperty
  private double criticalLostTaskPortion = 0.1;

  @JsonProperty
  private boolean includePreviousLostTaskCount = true;

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

  public boolean isCheckLateTasks() {
    return checkLateTasks;
  }

  public void setCheckLateTasks(boolean checkLateTasks) {
    this.checkLateTasks = checkLateTasks;
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

  public boolean isCheckLostTasks() {
    return checkLostTasks;
  }

  public void setCheckLostTasks(boolean checkLostTasks) {
    this.checkLostTasks = checkLostTasks;
  }

  public List<Reason> getLostTaskReasons() {
    return lostTaskReasons;
  }

  public void setLostTaskReasons(List<Reason> lostTaskReasons) {
    this.lostTaskReasons = lostTaskReasons;
  }

  public double getCriticalLostTaskPortion() {
    return criticalLostTaskPortion;
  }

  public void setCriticalLostTaskPortion(double criticalLostTaskPortion) {
    this.criticalLostTaskPortion = criticalLostTaskPortion;
  }

  public boolean isIncludePreviousLostTaskCount() {
    return includePreviousLostTaskCount;
  }

  public void setIncludePreviousLostTaskCount(boolean includePreviousLostTaskCount) {
    this.includePreviousLostTaskCount = includePreviousLostTaskCount;
  }
}
