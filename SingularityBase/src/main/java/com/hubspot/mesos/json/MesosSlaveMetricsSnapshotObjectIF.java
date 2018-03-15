package com.hubspot.mesos.json;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.singularity.annotations.SingularityStyle;

@Immutable
@SingularityStyle
public interface MesosSlaveMetricsSnapshotObjectIF {
  @JsonProperty("slave/disk_used")
  double getSlaveDiskUsed();

  @JsonProperty("slave/valid_status_updates")
  double getSlaveValidStatusUpdates();

  @JsonProperty("slave/tasks_finished")
  double getSlaveTasksFinished();

  @JsonProperty("slave/cpus_total")
  double getSlaveCpusTotal();

  @JsonProperty("slave/executors_preempted")
  double getSlaveExecutorsPreempted();

  @JsonProperty("slave/executors_terminated")
  double getSlaveExecutorsTerminated();

  @JsonProperty("slave/cpus_percent")
  double getSlaveCpusPercent();

  @JsonProperty("slave/executors_running")
  double getSlaveExecutorsRunning();

  @JsonProperty("slave/gpus_revocable_used")
  double getSlaveGpusRevocableUsed();

  @JsonProperty("slave/invalid_status_updates")
  double getSlaveInvalidStatusUpdates();

  @JsonProperty("slave/executors_registering")
  double getSlaveExecutorsRegistering();

  @JsonProperty("slave/mem_revocable_total")
  double getSlaveMemRevocableTotal();

  @JsonProperty("system/cpus_total")
  double getSystemCpusTotal();

  @JsonProperty("slave/frameworks_active")
  double getSlaveFrameworksActive();

  @JsonProperty("slave/cpus_revocable_percent")
  double getSlaveCpusRevocablePercent();

  @JsonProperty("slave/gpus_total")
  double getSlaveGpusTotal();

  @JsonProperty("slave/tasks_killed")
  double getSlaveTasksKilled();

  @JsonProperty("slave/tasks_starting")
  double getSlaveTasksStarting();

  @JsonProperty("slave/registered")
  double getSlaveRegistered();

  @JsonProperty("slave/gpus_revocable_total")
  double getSlaveGpusRevocableTotal();

  @JsonProperty("containerizer/mesos/provisioner/remove_container_errors")
  double getContainerizerMesosProvisionerRemoveContainerErrors();

  @JsonProperty("slave/cpus_revocable_used")
  double getSlaveCpusRevocableUsed();

  @JsonProperty("slave/disk_total")
  double getSlaveDiskTotal();

  @JsonProperty("slave/tasks_staging")
  double getSlaveTasksStaging();

  @JsonProperty("slave/gpus_used")
  double getSlaveGpusUsed();

  @JsonProperty("system/load_5min")
  double getSystemLoad5Min();

  @JsonProperty("slave/executor_directory_max_allowed_age_secs")
  double getSlaveExecutorDirectoryMaxAllowedAgeSecs();

  @JsonProperty("slave/disk_revocable_percent")
  double getSlaveDiskRevocablePercent();

  @JsonProperty("system/load_1min")
  double getSystemLoad1Min();

  @JsonProperty("slave/mem_percent")
  double getSlaveMemPercent();

  @JsonProperty("slave/container_launch_errors")
  double getSlaveContainerLaunchErrors();

  @JsonProperty("slave/mem_revocable_used")
  double getSlaveMemRevocableUsed();

  @JsonProperty("slave/tasks_running")
  double getSlaveTasksRunning();

  @JsonProperty("slave/invalid_framework_messages")
  double getSlaveInvalidFrameworkMessages();

  @JsonProperty("slave/valid_framework_messages")
  double getSlaveValidFrameworkMessages();

  @JsonProperty("slave/mem_revocable_percent")
  double getSlaveMemRevocablePercent();

  @JsonProperty("slave/tasks_failed")
  double getSlaveTasksFailed();

  @JsonProperty("slave/executors_terminating")
  double getSlaveExecutorsTerminating();

  @JsonProperty("slave/tasks_killing")
  double getSlaveTasksKilling();

  @JsonProperty("slave/disk_revocable_total")
  double getSlaveDiskRevocableTotal();

  @JsonProperty("slave/gpus_revocable_percent")
  double getSlaveGpusRevocablePercent();

  @JsonProperty("slave/disk_revocable_used")
  double getSlaveDiskRevocableUsed();

  @JsonProperty("slave/uptime_secs")
  double getSlaveUptimeSecs();

  @JsonProperty("slave/disk_percent")
  double getSlaveDiskPercent();

  @JsonProperty("slave/cpus_used")
  double getSlaveCpusUsed();

  @JsonProperty("system/mem_total_bytes")
  double getSystemMemTotalBytes();

  @JsonProperty("containerizer/mesos/provisioner/bind/remove_rootfs_errors")
  double getContainerizerMesosProvisionerBindRemoveRootfsErrors();

  @JsonProperty("system/mem_free_bytes")
  double getSystemMemFreeBytes();

  @JsonProperty("slave/cpus_revocable_total")
  double getSlaveCpusRevocableTotal();

  @JsonProperty("slave/tasks_lost")
  double getSlaveTasksLost();

  @JsonProperty("slave/gpus_percent")
  double getSlaveGpusPercent();

  @JsonProperty("system/load_15min")
  double getSystemLoad15Min();

  @JsonProperty("slave/recovery_errors")
  double getSlaveRecoveryErrors();

  @JsonProperty("slave/mem_used")
  double getSlaveMemUsed();

  @JsonProperty("slave/mem_total")
  double getSlaveMemTotal();

  @JsonProperty("containerizer/mesos/container_destroy_errors")
  double getContainerizerMesosContainerDestroyErrors();
}
