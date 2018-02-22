package com.hubspot.mesos.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MesosSlaveMetricsSnapshotObject {
  private final double slaveDiskUsed;
  private final double slaveValidStatusUpdates;
  private final double slaveTasksFinished;
  private final double slaveCpusTotal;
  private final double slaveExecutorsPreempted;
  private final double slaveExecutorsTerminated;
  private final double slaveCpusPercent;
  private final double slaveExecutorsRunning;
  private final double slaveGpusRevocableUsed;
  private final double slaveInvalidStatusUpdates;
  private final double slaveExecutorsRegistering;
  private final double slaveMemRevocableTotal;
  private final double systemCpusTotal;
  private final double slaveFrameworksActive;
  private final double slaveCpusRevocablePercent;
  private final double slaveGpusTotal;
  private final double slaveTasksKilled;
  private final double slaveTasksStarting;
  private final double slaveRegistered;
  private final double slaveGpusRevocableTotal;
  private final double containerizerMesosProvisionerRemoveContainerErrors;
  private final double slaveCpusRevocableUsed;
  private final double slaveDiskTotal;
  private final double slaveTasksStaging;
  private final double slaveGpusUsed;
  private final double systemLoad5Min;
  private final double slaveExecutorDirectoryMaxAllowedAgeSecs;
  private final double slaveDiskRevocablePercent;
  private final double systemLoad1Min;
  private final double slaveMemPercent;
  private final double slaveContainerLaunchErrors;
  private final double slaveMemRevocableUsed;
  private final double slaveTasksRunning;
  private final double slaveInvalidFrameworkMessages;
  private final double slaveValidFrameworkMessages;
  private final double slaveMemRevocablePercent;
  private final double slaveTasksFailed;
  private final double slaveExecutorsTerminating;
  private final double slaveTasksKilling;
  private final double slaveDiskRevocableTotal;
  private final double slaveGpusRevocablePercent;
  private final double slaveDiskRevocableUsed;
  private final double slaveUptimeSecs;
  private final double slaveDiskPercent;
  private final double slaveCpusUsed;
  private final double systemMemTotalBytes;
  private final double containerizerMesosProvisionerBindRemoveRootfsErrors;
  private final double systemMemFreeBytes;
  private final double slaveCpusRevocableTotal;
  private final double slaveTasksLost;
  private final double slaveGpusPercent;
  private final double systemLoad15Min;
  private final double slaveRecoveryErrors;
  private final double slaveMemUsed;
  private final double slaveMemTotal;
  private final double containerizerMesosContainerDestroyErrors;

  @JsonCreator
  public MesosSlaveMetricsSnapshotObject(@JsonProperty("slave/disk_used") double slaveDiskUsed,
                                         @JsonProperty("slave/valid_status_updates") double slaveValidStatusUpdates,
                                         @JsonProperty("slave/tasks_finished") double slaveTasksFinished,
                                         @JsonProperty("slave/cpus_total") double slaveCpusTotal,
                                         @JsonProperty("slave/executors_preempted") double slaveExecutorsPreempted,
                                         @JsonProperty("slave/executors_terminated") double slaveExecutorsTerminated,
                                         @JsonProperty("slave/cpus_percent") double slaveCpusPercent,
                                         @JsonProperty("slave/executors_running") double slaveExecutorsRunning,
                                         @JsonProperty("slave/gpus_revocable_used") double slaveGpusRevocableUsed,
                                         @JsonProperty("slave/invalid_status_updates") double slaveInvalidStatusUpdates,
                                         @JsonProperty("slave/executors_registering") double slaveExecutorsRegistering,
                                         @JsonProperty("slave/mem_revocable_total") double slaveMemRevocableTotal,
                                         @JsonProperty("system/cpus_total") double systemCpusTotal,
                                         @JsonProperty("slave/frameworks_active") double slaveFrameworksActive,
                                         @JsonProperty("slave/cpus_revocable_percent") double slaveCpusRevocablePercent,
                                         @JsonProperty("slave/gpus_total") double slaveGpusTotal,
                                         @JsonProperty("slave/tasks_killed") double slaveTasksKilled,
                                         @JsonProperty("slave/tasks_starting") double slaveTasksStarting,
                                         @JsonProperty("slave/registered") double slaveRegistered,
                                         @JsonProperty("slave/gpus_revocable_total") double slaveGpusRevocableTotal,
                                         @JsonProperty("containerizer/mesos/provisioner/remove_container_errors") double containerizerMesosProvisionerRemoveContainerErrors,
                                         @JsonProperty("slave/cpus_revocable_used") double slaveCpusRevocableUsed,
                                         @JsonProperty("slave/disk_total") double slaveDiskTotal,
                                         @JsonProperty("slave/tasks_staging") double slaveTasksStaging,
                                         @JsonProperty("slave/gpus_used") double slaveGpusUsed,
                                         @JsonProperty("system/load_5min") double systemLoad5Min,
                                         @JsonProperty("slave/executor_directory_max_allowed_age_secs") double slaveExecutorDirectoryMaxAllowedAgeSecs,
                                         @JsonProperty("slave/disk_revocable_percent") double slaveDiskRevocablePercent,
                                         @JsonProperty("system/load_1min") double systemLoad1Min,
                                         @JsonProperty("slave/mem_percent") double slaveMemPercent,
                                         @JsonProperty("slave/container_launch_errors") double slaveContainerLaunchErrors,
                                         @JsonProperty("slave/mem_revocable_used") double slaveMemRevocableUsed,
                                         @JsonProperty("slave/tasks_running") double slaveTasksRunning,
                                         @JsonProperty("slave/invalid_framework_messages") double slaveInvalidFrameworkMessages,
                                         @JsonProperty("slave/valid_framework_messages") double slaveValidFrameworkMessages,
                                         @JsonProperty("slave/mem_revocable_percent") double slaveMemRevocablePercent,
                                         @JsonProperty("slave/tasks_failed") double slaveTasksFailed,
                                         @JsonProperty("slave/executors_terminating") double slaveExecutorsTerminating,
                                         @JsonProperty("slave/tasks_killing") double slaveTasksKilling,
                                         @JsonProperty("slave/disk_revocable_total") double slaveDiskRevocableTotal,
                                         @JsonProperty("slave/gpus_revocable_percent") double slaveGpusRevocablePercent,
                                         @JsonProperty("slave/disk_revocable_used") double slaveDiskRevocableUsed,
                                         @JsonProperty("slave/uptime_secs") double slaveUptimeSecs,
                                         @JsonProperty("slave/disk_percent") double slaveDiskPercent,
                                         @JsonProperty("slave/cpus_used") double slaveCpusUsed,
                                         @JsonProperty("system/mem_total_bytes") double systemMemTotalBytes,
                                         @JsonProperty("containerizer/mesos/provisioner/bind/remove_rootfs_errors") double containerizerMesosProvisionerBindRemoveRootfsErrors,
                                         @JsonProperty("system/mem_free_bytes") double systemMemFreeBytes,
                                         @JsonProperty("slave/cpus_revocable_total") double slaveCpusRevocableTotal,
                                         @JsonProperty("slave/tasks_lost") double slaveTasksLost,
                                         @JsonProperty("slave/gpus_percent") double slaveGpusPercent,
                                         @JsonProperty("system/load_15min") double systemLoad15Min,
                                         @JsonProperty("slave/recovery_errors") double slaveRecoveryErrors,
                                         @JsonProperty("slave/mem_used") double slaveMemUsed,
                                         @JsonProperty("slave/mem_total") double slaveMemTotal,
                                         @JsonProperty("containerizer/mesos/container_destroy_errors") double containerizerMesosContainerDestroyErrors) {
    this.slaveDiskUsed = slaveDiskUsed;
    this.slaveValidStatusUpdates = slaveValidStatusUpdates;
    this.slaveTasksFinished = slaveTasksFinished;
    this.slaveCpusTotal = slaveCpusTotal;
    this.slaveExecutorsPreempted = slaveExecutorsPreempted;
    this.slaveExecutorsTerminated = slaveExecutorsTerminated;
    this.slaveCpusPercent = slaveCpusPercent;
    this.slaveExecutorsRunning = slaveExecutorsRunning;
    this.slaveGpusRevocableUsed = slaveGpusRevocableUsed;
    this.slaveInvalidStatusUpdates = slaveInvalidStatusUpdates;
    this.slaveExecutorsRegistering = slaveExecutorsRegistering;
    this.slaveMemRevocableTotal = slaveMemRevocableTotal;
    this.systemCpusTotal = systemCpusTotal;
    this.slaveFrameworksActive = slaveFrameworksActive;
    this.slaveCpusRevocablePercent = slaveCpusRevocablePercent;
    this.slaveGpusTotal = slaveGpusTotal;
    this.slaveTasksKilled = slaveTasksKilled;
    this.slaveTasksStarting = slaveTasksStarting;
    this.slaveRegistered = slaveRegistered;
    this.slaveGpusRevocableTotal = slaveGpusRevocableTotal;
    this.containerizerMesosProvisionerRemoveContainerErrors = containerizerMesosProvisionerRemoveContainerErrors;
    this.slaveCpusRevocableUsed = slaveCpusRevocableUsed;
    this.slaveDiskTotal = slaveDiskTotal;
    this.slaveTasksStaging = slaveTasksStaging;
    this.slaveGpusUsed = slaveGpusUsed;
    this.systemLoad5Min = systemLoad5Min;
    this.slaveExecutorDirectoryMaxAllowedAgeSecs = slaveExecutorDirectoryMaxAllowedAgeSecs;
    this.slaveDiskRevocablePercent = slaveDiskRevocablePercent;
    this.systemLoad1Min = systemLoad1Min;
    this.slaveMemPercent = slaveMemPercent;
    this.slaveContainerLaunchErrors = slaveContainerLaunchErrors;
    this.slaveMemRevocableUsed = slaveMemRevocableUsed;
    this.slaveTasksRunning = slaveTasksRunning;
    this.slaveInvalidFrameworkMessages = slaveInvalidFrameworkMessages;
    this.slaveValidFrameworkMessages = slaveValidFrameworkMessages;
    this.slaveMemRevocablePercent = slaveMemRevocablePercent;
    this.slaveTasksFailed = slaveTasksFailed;
    this.slaveExecutorsTerminating = slaveExecutorsTerminating;
    this.slaveTasksKilling = slaveTasksKilling;
    this.slaveDiskRevocableTotal = slaveDiskRevocableTotal;
    this.slaveGpusRevocablePercent = slaveGpusRevocablePercent;
    this.slaveDiskRevocableUsed = slaveDiskRevocableUsed;
    this.slaveUptimeSecs = slaveUptimeSecs;
    this.slaveDiskPercent = slaveDiskPercent;
    this.slaveCpusUsed = slaveCpusUsed;
    this.systemMemTotalBytes = systemMemTotalBytes;
    this.containerizerMesosProvisionerBindRemoveRootfsErrors = containerizerMesosProvisionerBindRemoveRootfsErrors;
    this.systemMemFreeBytes = systemMemFreeBytes;
    this.slaveCpusRevocableTotal = slaveCpusRevocableTotal;
    this.slaveTasksLost = slaveTasksLost;
    this.slaveGpusPercent = slaveGpusPercent;
    this.systemLoad15Min = systemLoad15Min;
    this.slaveRecoveryErrors = slaveRecoveryErrors;
    this.slaveMemUsed = slaveMemUsed;
    this.slaveMemTotal = slaveMemTotal;
    this.containerizerMesosContainerDestroyErrors = containerizerMesosContainerDestroyErrors;
  }

  public double getSlaveDiskUsed() {
    return slaveDiskUsed;
  }

  public double getSlaveValidStatusUpdates() {
    return slaveValidStatusUpdates;
  }

  public double getSlaveTasksFinished() {
    return slaveTasksFinished;
  }

  public double getSlaveCpusTotal() {
    return slaveCpusTotal;
  }

  public double getSlaveExecutorsPreempted() {
    return slaveExecutorsPreempted;
  }

  public double getSlaveExecutorsTerminated() {
    return slaveExecutorsTerminated;
  }

  public double getSlaveCpusPercent() {
    return slaveCpusPercent;
  }

  public double getSlaveExecutorsRunning() {
    return slaveExecutorsRunning;
  }

  public double getSlaveGpusRevocableUsed() {
    return slaveGpusRevocableUsed;
  }

  public double getSlaveInvalidStatusUpdates() {
    return slaveInvalidStatusUpdates;
  }

  public double getSlaveExecutorsRegistering() {
    return slaveExecutorsRegistering;
  }

  public double getSlaveMemRevocableTotal() {
    return slaveMemRevocableTotal;
  }

  public double getSystemCpusTotal() {
    return systemCpusTotal;
  }

  public double getSlaveFrameworksActive() {
    return slaveFrameworksActive;
  }

  public double getSlaveCpusRevocablePercent() {
    return slaveCpusRevocablePercent;
  }

  public double getSlaveGpusTotal() {
    return slaveGpusTotal;
  }

  public double getSlaveTasksKilled() {
    return slaveTasksKilled;
  }

  public double getSlaveTasksStarting() {
    return slaveTasksStarting;
  }

  public double getSlaveRegistered() {
    return slaveRegistered;
  }

  public double getSlaveGpusRevocableTotal() {
    return slaveGpusRevocableTotal;
  }

  public double getContainerizerMesosProvisionerRemoveContainerErrors() {
    return containerizerMesosProvisionerRemoveContainerErrors;
  }

  public double getSlaveCpusRevocableUsed() {
    return slaveCpusRevocableUsed;
  }

  public double getSlaveDiskTotal() {
    return slaveDiskTotal;
  }

  public double getSlaveTasksStaging() {
    return slaveTasksStaging;
  }

  public double getSlaveGpusUsed() {
    return slaveGpusUsed;
  }

  public double getSystemLoad5Min() {
    return systemLoad5Min;
  }

  public double getSlaveExecutorDirectoryMaxAllowedAgeSecs() {
    return slaveExecutorDirectoryMaxAllowedAgeSecs;
  }

  public double getSlaveDiskRevocablePercent() {
    return slaveDiskRevocablePercent;
  }

  public double getSystemLoad1Min() {
    return systemLoad1Min;
  }

  public double getSlaveMemPercent() {
    return slaveMemPercent;
  }

  public double getSlaveContainerLaunchErrors() {
    return slaveContainerLaunchErrors;
  }

  public double getSlaveMemRevocableUsed() {
    return slaveMemRevocableUsed;
  }

  public double getSlaveTasksRunning() {
    return slaveTasksRunning;
  }

  public double getSlaveInvalidFrameworkMessages() {
    return slaveInvalidFrameworkMessages;
  }

  public double getSlaveValidFrameworkMessages() {
    return slaveValidFrameworkMessages;
  }

  public double getSlaveMemRevocablePercent() {
    return slaveMemRevocablePercent;
  }

  public double getSlaveTasksFailed() {
    return slaveTasksFailed;
  }

  public double getSlaveExecutorsTerminating() {
    return slaveExecutorsTerminating;
  }

  public double getSlaveTasksKilling() {
    return slaveTasksKilling;
  }

  public double getSlaveDiskRevocableTotal() {
    return slaveDiskRevocableTotal;
  }

  public double getSlaveGpusRevocablePercent() {
    return slaveGpusRevocablePercent;
  }

  public double getSlaveDiskRevocableUsed() {
    return slaveDiskRevocableUsed;
  }

  public double getSlaveUptimeSecs() {
    return slaveUptimeSecs;
  }

  public double getSlaveDiskPercent() {
    return slaveDiskPercent;
  }

  public double getSlaveCpusUsed() {
    return slaveCpusUsed;
  }

  public double getSystemMemTotalBytes() {
    return systemMemTotalBytes;
  }

  public double getContainerizerMesosProvisionerBindRemoveRootfsErrors() {
    return containerizerMesosProvisionerBindRemoveRootfsErrors;
  }

  public double getSystemMemFreeBytes() {
    return systemMemFreeBytes;
  }

  public double getSlaveCpusRevocableTotal() {
    return slaveCpusRevocableTotal;
  }

  public double getSlaveTasksLost() {
    return slaveTasksLost;
  }

  public double getSlaveGpusPercent() {
    return slaveGpusPercent;
  }

  public double getSystemLoad15Min() {
    return systemLoad15Min;
  }

  public double getSlaveRecoveryErrors() {
    return slaveRecoveryErrors;
  }

  public double getSlaveMemUsed() {
    return slaveMemUsed;
  }

  public double getSlaveMemTotal() {
    return slaveMemTotal;
  }

  public double getContainerizerMesosContainerDestroyErrors() {
    return containerizerMesosContainerDestroyErrors;
  }

  @Override
  public String toString() {
    return "MesosSlaveMetricsSnapshotObject{" +
        "slaveDiskUsed=" + slaveDiskUsed +
        ", slaveValidStatusUpdates=" + slaveValidStatusUpdates +
        ", slaveTasksFinished=" + slaveTasksFinished +
        ", slaveCpusTotal=" + slaveCpusTotal +
        ", slaveExecutorsPreempted=" + slaveExecutorsPreempted +
        ", slaveExecutorsTerminated=" + slaveExecutorsTerminated +
        ", slaveCpusPercent=" + slaveCpusPercent +
        ", slaveExecutorsRunning=" + slaveExecutorsRunning +
        ", slaveGpusRevocableUsed=" + slaveGpusRevocableUsed +
        ", slaveInvalidStatusUpdates=" + slaveInvalidStatusUpdates +
        ", slaveExecutorsRegistering=" + slaveExecutorsRegistering +
        ", slaveMemRevocableTotal=" + slaveMemRevocableTotal +
        ", systemCpusTotal=" + systemCpusTotal +
        ", slaveFrameworksActive=" + slaveFrameworksActive +
        ", slaveCpusRevocablePercent=" + slaveCpusRevocablePercent +
        ", slaveGpusTotal=" + slaveGpusTotal +
        ", slaveTasksKilled=" + slaveTasksKilled +
        ", slaveTasksStarting=" + slaveTasksStarting +
        ", slaveRegistered=" + slaveRegistered +
        ", slaveGpusRevocableTotal=" + slaveGpusRevocableTotal +
        ", containerizerMesosProvisionerRemoveContainerErrors=" + containerizerMesosProvisionerRemoveContainerErrors +
        ", slaveCpusRevocableUsed=" + slaveCpusRevocableUsed +
        ", slaveDiskTotal=" + slaveDiskTotal +
        ", slaveTasksStaging=" + slaveTasksStaging +
        ", slaveGpusUsed=" + slaveGpusUsed +
        ", systemLoad5Min=" + systemLoad5Min +
        ", slaveExecutorDirectoryMaxAllowedAgeSecs=" + slaveExecutorDirectoryMaxAllowedAgeSecs +
        ", slaveDiskRevocablePercent=" + slaveDiskRevocablePercent +
        ", systemLoad1Min=" + systemLoad1Min +
        ", slaveMemPercent=" + slaveMemPercent +
        ", slaveContainerLaunchErrors=" + slaveContainerLaunchErrors +
        ", slaveMemRevocableUsed=" + slaveMemRevocableUsed +
        ", slaveTasksRunning=" + slaveTasksRunning +
        ", slaveInvalidFrameworkMessages=" + slaveInvalidFrameworkMessages +
        ", slaveValidFrameworkMessages=" + slaveValidFrameworkMessages +
        ", slaveMemRevocablePercent=" + slaveMemRevocablePercent +
        ", slaveTasksFailed=" + slaveTasksFailed +
        ", slaveExecutorsTerminating=" + slaveExecutorsTerminating +
        ", slaveTasksKilling=" + slaveTasksKilling +
        ", slaveDiskRevocableTotal=" + slaveDiskRevocableTotal +
        ", slaveGpusRevocablePercent=" + slaveGpusRevocablePercent +
        ", slaveDiskRevocableUsed=" + slaveDiskRevocableUsed +
        ", slaveUptimeSecs=" + slaveUptimeSecs +
        ", slaveDiskPercent=" + slaveDiskPercent +
        ", slaveCpusUsed=" + slaveCpusUsed +
        ", systemMemTotalBytes=" + systemMemTotalBytes +
        ", containerizerMesosProvisionerBindRemoveRootfsErrors=" + containerizerMesosProvisionerBindRemoveRootfsErrors +
        ", systemMemFreeBytes=" + systemMemFreeBytes +
        ", slaveCpusRevocableTotal=" + slaveCpusRevocableTotal +
        ", slaveTasksLost=" + slaveTasksLost +
        ", slaveGpusPercent=" + slaveGpusPercent +
        ", systemLoad15Min=" + systemLoad15Min +
        ", slaveRecoveryErrors=" + slaveRecoveryErrors +
        ", slaveMemUsed=" + slaveMemUsed +
        ", slaveMemTotal=" + slaveMemTotal +
        ", containerizerMesosContainerDestroyErrors=" + containerizerMesosContainerDestroyErrors +
        '}';
  }
}
