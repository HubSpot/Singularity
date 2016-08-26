package com.hubspot.singularity.config;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

import org.apache.mesos.Protos.TaskStatus.Reason;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.hubspot.singularity.SingularityDisabledActionType;

public class DisasterDetectionConfiguration {

  private boolean enabled = false;

  private long runEveryMillis = TimeUnit.SECONDS.toMillis(30);

  @JsonProperty("disableActionsOnDisaster")
  @NotNull
  private List<SingularityDisabledActionType> disableActionsOnDisaster = Collections.emptyList();

  private boolean checkLateTasks = true;

  private long criticalAvgTaskLagMillis = 240000L;

  private double criticalOverdueTaskPortion = 0.1;

  private boolean checkLostSlaves = true;

  private double criticalLostSlavePortion = 0.2;

  private boolean includePreviousLostSlavesCount = true;

  private boolean checkLostTasks = true;

  @JsonProperty("lostTaskReasons")
  @NotNull
  private List<Reason> lostTaskReasons = ImmutableList.of(
    Reason.REASON_INVALID_OFFERS, Reason.REASON_SLAVE_UNKNOWN, Reason.REASON_SLAVE_REMOVED, Reason.REASON_SLAVE_RESTARTED, Reason.REASON_MASTER_DISCONNECTED);

  private double criticalLostTaskPortion = 0.2;

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

  public boolean isIncludePreviousLostSlavesCount() {
    return includePreviousLostSlavesCount;
  }

  public void setIncludePreviousLostSlavesCount(boolean includePreviousLostSlavesCount) {
    this.includePreviousLostSlavesCount = includePreviousLostSlavesCount;
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
