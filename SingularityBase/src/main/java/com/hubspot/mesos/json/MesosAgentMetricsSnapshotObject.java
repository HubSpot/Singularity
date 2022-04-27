package com.hubspot.mesos.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MesosAgentMetricsSnapshotObject {

  private final double diskUsed;
  private final double validStatusUpdates;
  private final double tasksFinished;
  private final double cpusTotal;
  private final double executorsPreempted;
  private final double executorsTerminated;
  private final double cpusPercent;
  private final double executorsRunning;
  private final double gpusRevocableUsed;
  private final double invalidStatusUpdates;
  private final double executorsRegistering;
  private final double memRevocableTotal;
  private final double systemCpusTotal;
  private final double frameworksActive;
  private final double cpusRevocablePercent;
  private final double gpusTotal;
  private final double tasksKilled;
  private final double tasksStarting;
  private final double registered;
  private final double gpusRevocableTotal;
  private final double containerizerMesosProvisionerRemoveContainerErrors;
  private final double cpusRevocableUsed;
  private final double diskTotal;
  private final double tasksStaging;
  private final double gpusUsed;
  private final double systemLoad5Min;
  private final double executorDirectoryMaxAllowedAgeSecs;
  private final double diskRevocablePercent;
  private final double systemLoad1Min;
  private final double memPercent;
  private final double containerLaunchErrors;
  private final double memRevocableUsed;
  private final double tasksRunning;
  private final double invalidFrameworkMessages;
  private final double validFrameworkMessages;
  private final double memRevocablePercent;
  private final double tasksFailed;
  private final double executorsTerminating;
  private final double tasksKilling;
  private final double diskRevocableTotal;
  private final double gpusRevocablePercent;
  private final double diskRevocableUsed;
  private final double uptimeSecs;
  private final double diskPercent;
  private final double cpusUsed;
  private final double systemMemTotalBytes;
  private final double containerizerMesosProvisionerBindRemoveRootfsErrors;
  private final double systemMemFreeBytes;
  private final double cpusRevocableTotal;
  private final double tasksLost;
  private final double gpusPercent;
  private final double systemLoad15Min;
  private final double recoveryErrors;
  private final double memUsed;
  private final double memTotal;
  private final double containerizerMesosContainerDestroyErrors;

  @JsonCreator
  public MesosAgentMetricsSnapshotObject(
    @JsonProperty("slave/disk_used") double diskUsed,
    @JsonProperty("slave/valid_status_updates") double validStatusUpdates,
    @JsonProperty("slave/tasks_finished") double tasksFinished,
    @JsonProperty("slave/cpus_total") double cpusTotal,
    @JsonProperty("slave/executors_preempted") double executorsPreempted,
    @JsonProperty("slave/executors_terminated") double executorsTerminated,
    @JsonProperty("slave/cpus_percent") double cpusPercent,
    @JsonProperty("slave/executors_running") double executorsRunning,
    @JsonProperty("slave/gpus_revocable_used") double gpusRevocableUsed,
    @JsonProperty("slave/invalid_status_updates") double invalidStatusUpdates,
    @JsonProperty("slave/executors_registering") double executorsRegistering,
    @JsonProperty("slave/mem_revocable_total") double memRevocableTotal,
    @JsonProperty("system/cpus_total") double systemCpusTotal,
    @JsonProperty("slave/frameworks_active") double frameworksActive,
    @JsonProperty("slave/cpus_revocable_percent") double cpusRevocablePercent,
    @JsonProperty("slave/gpus_total") double gpusTotal,
    @JsonProperty("slave/tasks_killed") double tasksKilled,
    @JsonProperty("slave/tasks_starting") double tasksStarting,
    @JsonProperty("slave/registered") double registered,
    @JsonProperty("slave/gpus_revocable_total") double gpusRevocableTotal,
    @JsonProperty(
      "containerizer/mesos/provisioner/remove_container_errors"
    ) double containerizerMesosProvisionerRemoveContainerErrors,
    @JsonProperty("slave/cpus_revocable_used") double cpusRevocableUsed,
    @JsonProperty("slave/disk_total") double diskTotal,
    @JsonProperty("slave/tasks_staging") double tasksStaging,
    @JsonProperty("slave/gpus_used") double gpusUsed,
    @JsonProperty("system/load_5min") double systemLoad5Min,
    @JsonProperty(
      "slave/executor_directory_max_allowed_age_secs"
    ) double executorDirectoryMaxAllowedAgeSecs,
    @JsonProperty("slave/disk_revocable_percent") double diskRevocablePercent,
    @JsonProperty("system/load_1min") double systemLoad1Min,
    @JsonProperty("slave/mem_percent") double memPercent,
    @JsonProperty("slave/container_launch_errors") double containerLaunchErrors,
    @JsonProperty("slave/mem_revocable_used") double memRevocableUsed,
    @JsonProperty("slave/tasks_running") double tasksRunning,
    @JsonProperty("slave/invalid_framework_messages") double invalidFrameworkMessages,
    @JsonProperty("slave/valid_framework_messages") double validFrameworkMessages,
    @JsonProperty("slave/mem_revocable_percent") double memRevocablePercent,
    @JsonProperty("slave/tasks_failed") double tasksFailed,
    @JsonProperty("slave/executors_terminating") double executorsTerminating,
    @JsonProperty("slave/tasks_killing") double tasksKilling,
    @JsonProperty("slave/disk_revocable_total") double diskRevocableTotal,
    @JsonProperty("slave/gpus_revocable_percent") double gpusRevocablePercent,
    @JsonProperty("slave/disk_revocable_used") double diskRevocableUsed,
    @JsonProperty("slave/uptime_secs") double uptimeSecs,
    @JsonProperty("slave/disk_percent") double diskPercent,
    @JsonProperty("slave/cpus_used") double cpusUsed,
    @JsonProperty("system/mem_total_bytes") double systemMemTotalBytes,
    @JsonProperty(
      "containerizer/mesos/provisioner/bind/remove_rootfs_errors"
    ) double containerizerMesosProvisionerBindRemoveRootfsErrors,
    @JsonProperty("system/mem_free_bytes") double systemMemFreeBytes,
    @JsonProperty("slave/cpus_revocable_total") double cpusRevocableTotal,
    @JsonProperty("slave/tasks_lost") double tasksLost,
    @JsonProperty("slave/gpus_percent") double gpusPercent,
    @JsonProperty("system/load_15min") double systemLoad15Min,
    @JsonProperty("slave/recovery_errors") double recoveryErrors,
    @JsonProperty("slave/mem_used") double memUsed,
    @JsonProperty("slave/mem_total") double memTotal,
    @JsonProperty(
      "containerizer/mesos/container_destroy_errors"
    ) double containerizerMesosContainerDestroyErrors
  ) {
    this.diskUsed = diskUsed;
    this.validStatusUpdates = validStatusUpdates;
    this.tasksFinished = tasksFinished;
    this.cpusTotal = cpusTotal;
    this.executorsPreempted = executorsPreempted;
    this.executorsTerminated = executorsTerminated;
    this.cpusPercent = cpusPercent;
    this.executorsRunning = executorsRunning;
    this.gpusRevocableUsed = gpusRevocableUsed;
    this.invalidStatusUpdates = invalidStatusUpdates;
    this.executorsRegistering = executorsRegistering;
    this.memRevocableTotal = memRevocableTotal;
    this.systemCpusTotal = systemCpusTotal;
    this.frameworksActive = frameworksActive;
    this.cpusRevocablePercent = cpusRevocablePercent;
    this.gpusTotal = gpusTotal;
    this.tasksKilled = tasksKilled;
    this.tasksStarting = tasksStarting;
    this.registered = registered;
    this.gpusRevocableTotal = gpusRevocableTotal;
    this.containerizerMesosProvisionerRemoveContainerErrors =
      containerizerMesosProvisionerRemoveContainerErrors;
    this.cpusRevocableUsed = cpusRevocableUsed;
    this.diskTotal = diskTotal;
    this.tasksStaging = tasksStaging;
    this.gpusUsed = gpusUsed;
    this.systemLoad5Min = systemLoad5Min;
    this.executorDirectoryMaxAllowedAgeSecs = executorDirectoryMaxAllowedAgeSecs;
    this.diskRevocablePercent = diskRevocablePercent;
    this.systemLoad1Min = systemLoad1Min;
    this.memPercent = memPercent;
    this.containerLaunchErrors = containerLaunchErrors;
    this.memRevocableUsed = memRevocableUsed;
    this.tasksRunning = tasksRunning;
    this.invalidFrameworkMessages = invalidFrameworkMessages;
    this.validFrameworkMessages = validFrameworkMessages;
    this.memRevocablePercent = memRevocablePercent;
    this.tasksFailed = tasksFailed;
    this.executorsTerminating = executorsTerminating;
    this.tasksKilling = tasksKilling;
    this.diskRevocableTotal = diskRevocableTotal;
    this.gpusRevocablePercent = gpusRevocablePercent;
    this.diskRevocableUsed = diskRevocableUsed;
    this.uptimeSecs = uptimeSecs;
    this.diskPercent = diskPercent;
    this.cpusUsed = cpusUsed;
    this.systemMemTotalBytes = systemMemTotalBytes;
    this.containerizerMesosProvisionerBindRemoveRootfsErrors =
      containerizerMesosProvisionerBindRemoveRootfsErrors;
    this.systemMemFreeBytes = systemMemFreeBytes;
    this.cpusRevocableTotal = cpusRevocableTotal;
    this.tasksLost = tasksLost;
    this.gpusPercent = gpusPercent;
    this.systemLoad15Min = systemLoad15Min;
    this.recoveryErrors = recoveryErrors;
    this.memUsed = memUsed;
    this.memTotal = memTotal;
    this.containerizerMesosContainerDestroyErrors =
      containerizerMesosContainerDestroyErrors;
  }

  public double getDiskUsed() {
    return diskUsed;
  }

  public double getValidStatusUpdates() {
    return validStatusUpdates;
  }

  public double getTasksFinished() {
    return tasksFinished;
  }

  public double getCpusTotal() {
    return cpusTotal;
  }

  public double getExecutorsPreempted() {
    return executorsPreempted;
  }

  public double getExecutorsTerminated() {
    return executorsTerminated;
  }

  public double getCpusPercent() {
    return cpusPercent;
  }

  public double getExecutorsRunning() {
    return executorsRunning;
  }

  public double getGpusRevocableUsed() {
    return gpusRevocableUsed;
  }

  public double getInvalidStatusUpdates() {
    return invalidStatusUpdates;
  }

  public double getExecutorsRegistering() {
    return executorsRegistering;
  }

  public double getMemRevocableTotal() {
    return memRevocableTotal;
  }

  public double getSystemCpusTotal() {
    return systemCpusTotal;
  }

  public double getFrameworksActive() {
    return frameworksActive;
  }

  public double getCpusRevocablePercent() {
    return cpusRevocablePercent;
  }

  public double getGpusTotal() {
    return gpusTotal;
  }

  public double getTasksKilled() {
    return tasksKilled;
  }

  public double getTasksStarting() {
    return tasksStarting;
  }

  public double getRegistered() {
    return registered;
  }

  public double getGpusRevocableTotal() {
    return gpusRevocableTotal;
  }

  public double getContainerizerMesosProvisionerRemoveContainerErrors() {
    return containerizerMesosProvisionerRemoveContainerErrors;
  }

  public double getCpusRevocableUsed() {
    return cpusRevocableUsed;
  }

  public double getDiskTotal() {
    return diskTotal;
  }

  public double getTasksStaging() {
    return tasksStaging;
  }

  public double getGpusUsed() {
    return gpusUsed;
  }

  public double getSystemLoad5Min() {
    return systemLoad5Min;
  }

  public double getExecutorDirectoryMaxAllowedAgeSecs() {
    return executorDirectoryMaxAllowedAgeSecs;
  }

  public double getDiskRevocablePercent() {
    return diskRevocablePercent;
  }

  public double getSystemLoad1Min() {
    return systemLoad1Min;
  }

  public double getMemPercent() {
    return memPercent;
  }

  public double getContainerLaunchErrors() {
    return containerLaunchErrors;
  }

  public double getMemRevocableUsed() {
    return memRevocableUsed;
  }

  public double getTasksRunning() {
    return tasksRunning;
  }

  public double getInvalidFrameworkMessages() {
    return invalidFrameworkMessages;
  }

  public double getValidFrameworkMessages() {
    return validFrameworkMessages;
  }

  public double getMemRevocablePercent() {
    return memRevocablePercent;
  }

  public double getTasksFailed() {
    return tasksFailed;
  }

  public double getExecutorsTerminating() {
    return executorsTerminating;
  }

  public double getTasksKilling() {
    return tasksKilling;
  }

  public double getDiskRevocableTotal() {
    return diskRevocableTotal;
  }

  public double getGpusRevocablePercent() {
    return gpusRevocablePercent;
  }

  public double getDiskRevocableUsed() {
    return diskRevocableUsed;
  }

  public double getUptimeSecs() {
    return uptimeSecs;
  }

  public double getDiskPercent() {
    return diskPercent;
  }

  public double getCpusUsed() {
    return cpusUsed;
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

  public double getCpusRevocableTotal() {
    return cpusRevocableTotal;
  }

  public double getTasksLost() {
    return tasksLost;
  }

  public double getGpusPercent() {
    return gpusPercent;
  }

  public double getSystemLoad15Min() {
    return systemLoad15Min;
  }

  public double getRecoveryErrors() {
    return recoveryErrors;
  }

  public double getMemUsed() {
    return memUsed;
  }

  public double getMemTotal() {
    return memTotal;
  }

  public double getContainerizerMesosContainerDestroyErrors() {
    return containerizerMesosContainerDestroyErrors;
  }

  @Override
  public String toString() {
    return (
      "MesosAgentMetricsSnapshotObject{" +
      "diskUsed=" +
      diskUsed +
      ", validStatusUpdates=" +
      validStatusUpdates +
      ", tasksFinished=" +
      tasksFinished +
      ", cpusTotal=" +
      cpusTotal +
      ", executorsPreempted=" +
      executorsPreempted +
      ", executorsTerminated=" +
      executorsTerminated +
      ", cpusPercent=" +
      cpusPercent +
      ", executorsRunning=" +
      executorsRunning +
      ", gpusRevocableUsed=" +
      gpusRevocableUsed +
      ", invalidStatusUpdates=" +
      invalidStatusUpdates +
      ", executorsRegistering=" +
      executorsRegistering +
      ", memRevocableTotal=" +
      memRevocableTotal +
      ", systemCpusTotal=" +
      systemCpusTotal +
      ", frameworksActive=" +
      frameworksActive +
      ", cpusRevocablePercent=" +
      cpusRevocablePercent +
      ", gpusTotal=" +
      gpusTotal +
      ", tasksKilled=" +
      tasksKilled +
      ", tasksStarting=" +
      tasksStarting +
      ", registered=" +
      registered +
      ", gpusRevocableTotal=" +
      gpusRevocableTotal +
      ", containerizerMesosProvisionerRemoveContainerErrors=" +
      containerizerMesosProvisionerRemoveContainerErrors +
      ", cpusRevocableUsed=" +
      cpusRevocableUsed +
      ", diskTotal=" +
      diskTotal +
      ", tasksStaging=" +
      tasksStaging +
      ", gpusUsed=" +
      gpusUsed +
      ", systemLoad5Min=" +
      systemLoad5Min +
      ", executorDirectoryMaxAllowedAgeSecs=" +
      executorDirectoryMaxAllowedAgeSecs +
      ", diskRevocablePercent=" +
      diskRevocablePercent +
      ", systemLoad1Min=" +
      systemLoad1Min +
      ", memPercent=" +
      memPercent +
      ", containerLaunchErrors=" +
      containerLaunchErrors +
      ", memRevocableUsed=" +
      memRevocableUsed +
      ", tasksRunning=" +
      tasksRunning +
      ", invalidFrameworkMessages=" +
      invalidFrameworkMessages +
      ", validFrameworkMessages=" +
      validFrameworkMessages +
      ", memRevocablePercent=" +
      memRevocablePercent +
      ", tasksFailed=" +
      tasksFailed +
      ", executorsTerminating=" +
      executorsTerminating +
      ", tasksKilling=" +
      tasksKilling +
      ", diskRevocableTotal=" +
      diskRevocableTotal +
      ", gpusRevocablePercent=" +
      gpusRevocablePercent +
      ", diskRevocableUsed=" +
      diskRevocableUsed +
      ", uptimeSecs=" +
      uptimeSecs +
      ", diskPercent=" +
      diskPercent +
      ", cpusUsed=" +
      cpusUsed +
      ", systemMemTotalBytes=" +
      systemMemTotalBytes +
      ", containerizerMesosProvisionerBindRemoveRootfsErrors=" +
      containerizerMesosProvisionerBindRemoveRootfsErrors +
      ", systemMemFreeBytes=" +
      systemMemFreeBytes +
      ", cpusRevocableTotal=" +
      cpusRevocableTotal +
      ", tasksLost=" +
      tasksLost +
      ", gpusPercent=" +
      gpusPercent +
      ", systemLoad15Min=" +
      systemLoad15Min +
      ", recoveryErrors=" +
      recoveryErrors +
      ", memUsed=" +
      memUsed +
      ", memTotal=" +
      memTotal +
      ", containerizerMesosContainerDestroyErrors=" +
      containerizerMesosContainerDestroyErrors +
      '}'
    );
  }
}
