package com.hubspot.singularity.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.hubspot.singularity.SingularityAction;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;
import org.apache.mesos.v1.Protos.TaskStatus.Reason;

public class DisasterDetectionConfiguration {

  private boolean enabled = true;

  private int statsHistorySize = 10;

  private long runEveryMillis = TimeUnit.SECONDS.toMillis(10);

  private long defaultDisabledActionExpiration = TimeUnit.MINUTES.toMillis(15);

  @JsonProperty("disableActionsOnDisaster")
  @NotNull
  private List<SingularityAction> disableActionsOnDisaster = Collections.emptyList();

  private boolean checkLateTasks = true;

  private long warningAvgTaskLagMillis = TimeUnit.MINUTES.toMillis(2);

  private double warningOverdueTaskPortion = 0.05;

  private long criticalAvgTaskLagMillis = TimeUnit.MINUTES.toMillis(4);

  private double criticalOverdueTaskPortion = 0.1;

  private long triggerAfterMillisOverTaskLagThreshold = TimeUnit.SECONDS.toMillis(45);

  private boolean checkLostAgents = true;

  private double criticalLostAgentPortion = 0.2;

  private long includeLostAgentsInLastMillis = TimeUnit.SECONDS.toMillis(30);

  private boolean checkLostTasks = true;

  @JsonProperty("lostTaskReasons")
  @NotNull
  private List<Reason> lostTaskReasons = ImmutableList.of(
    Reason.REASON_INVALID_OFFERS,
    Reason.REASON_AGENT_UNKNOWN,
    Reason.REASON_AGENT_REMOVED,
    Reason.REASON_AGENT_RESTARTED,
    Reason.REASON_MASTER_DISCONNECTED
  );

  private double criticalLostTaskPortion = 0.2;

  private long includeLostTasksInLastMillis = TimeUnit.SECONDS.toMillis(30);

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

  public long getDefaultDisabledActionExpiration() {
    return defaultDisabledActionExpiration;
  }

  public void setDefaultDisabledActionExpiration(long defaultDisabledActionExpiration) {
    this.defaultDisabledActionExpiration = defaultDisabledActionExpiration;
  }

  public int getStatsHistorySize() {
    return statsHistorySize;
  }

  public void setStatsHistorySize(int statsHistorySize) {
    this.statsHistorySize = statsHistorySize;
  }

  public List<SingularityAction> getDisableActionsOnDisaster() {
    return disableActionsOnDisaster;
  }

  public void setDisableActionsOnDisaster(
    List<SingularityAction> disableActionsOnDisaster
  ) {
    this.disableActionsOnDisaster = disableActionsOnDisaster;
  }

  public boolean isCheckLateTasks() {
    return checkLateTasks;
  }

  public void setCheckLateTasks(boolean checkLateTasks) {
    this.checkLateTasks = checkLateTasks;
  }

  public long getWarningAvgTaskLagMillis() {
    return warningAvgTaskLagMillis;
  }

  public void setWarningAvgTaskLagMillis(long warningAvgTaskLagMillis) {
    this.warningAvgTaskLagMillis = warningAvgTaskLagMillis;
  }

  public double getWarningOverdueTaskPortion() {
    return warningOverdueTaskPortion;
  }

  public void setWarningOverdueTaskPortion(double warningOverdueTaskPortion) {
    this.warningOverdueTaskPortion = warningOverdueTaskPortion;
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

  public long getTriggerAfterMillisOverTaskLagThreshold() {
    return triggerAfterMillisOverTaskLagThreshold;
  }

  public void setTriggerAfterMillisOverTaskLagThreshold(
    long triggerAfterMillisOverTaskLagThreshold
  ) {
    this.triggerAfterMillisOverTaskLagThreshold = triggerAfterMillisOverTaskLagThreshold;
  }

  public boolean isCheckLostAgents() {
    return checkLostAgents;
  }

  public void setCheckLostAgents(boolean checkLostAgents) {
    this.checkLostAgents = checkLostAgents;
  }

  public double getCriticalLostAgentPortion() {
    return criticalLostAgentPortion;
  }

  public void setCriticalLostAgentPortion(double criticalLostAgentPortion) {
    this.criticalLostAgentPortion = criticalLostAgentPortion;
  }

  public long getIncludeLostAgentsInLastMillis() {
    return includeLostAgentsInLastMillis;
  }

  public void setIncludeLostAgentsInLastMillis(long includeLostAgentsInLastMillis) {
    this.includeLostAgentsInLastMillis = includeLostAgentsInLastMillis;
  }

  @Deprecated
  public boolean isCheckLostSlaves() {
    return checkLostAgents;
  }

  @Deprecated
  public void setCheckLostSlaves(boolean checkLostAgents) {
    this.checkLostAgents = checkLostAgents;
  }

  @Deprecated
  public double getCriticalLostSlavePortion() {
    return criticalLostAgentPortion;
  }

  @Deprecated
  public void setCriticalLostSlavePortion(double criticalLostAgentPortion) {
    this.criticalLostAgentPortion = criticalLostAgentPortion;
  }

  @Deprecated
  public long getIncludeLostSlavesInLastMillis() {
    return includeLostAgentsInLastMillis;
  }

  @Deprecated
  public void setIncludeLostSlavesInLastMillis(long includeLostAgentsInLastMillis) {
    this.includeLostAgentsInLastMillis = includeLostAgentsInLastMillis;
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

  public long getIncludeLostTasksInLastMillis() {
    return includeLostTasksInLastMillis;
  }

  public void setIncludeLostTasksInLastMillis(long includeLostTasksInLastMillis) {
    this.includeLostTasksInLastMillis = includeLostTasksInLastMillis;
  }
}
