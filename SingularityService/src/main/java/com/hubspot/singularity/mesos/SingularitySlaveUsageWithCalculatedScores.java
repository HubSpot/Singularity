package com.hubspot.singularity.mesos;

import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularitySlaveUsage.ResourceUsageType;

class SingularitySlaveUsageWithCalculatedScores {
  private final SingularitySlaveUsage slaveUsage;
  private final boolean missingUsageData;
  private final double longRunningCpusUsedScore;
  private final double longRunningMemUsedScore;
  private final double longRunningDiskUsedScore;
  private final double cpusFreeScore;
  private final double memFreeScore;
  private final double diskFreeScore;

  private final double defaultLongRunningTaskScore;

  public SingularitySlaveUsageWithCalculatedScores(SingularitySlaveUsage slaveUsage,
                                                   boolean missingUsageData,
                                                   double longRunningCpusUsedScore,
                                                   double longRunningMemUsedScore,
                                                   double longRunningDiskUsedScore,
                                                   double cpusFreeScore,
                                                   double memFreeScore,
                                                   double diskFreeScore,
                                                   double defaultLongRunningTaskScore) {
    this.slaveUsage = slaveUsage;
    this.missingUsageData = missingUsageData;
    this.longRunningCpusUsedScore = longRunningCpusUsedScore;
    this.longRunningMemUsedScore = longRunningMemUsedScore;
    this.longRunningDiskUsedScore = longRunningDiskUsedScore;
    this.cpusFreeScore = cpusFreeScore;
    this.memFreeScore = memFreeScore;
    this.diskFreeScore = diskFreeScore;
    this.defaultLongRunningTaskScore = defaultLongRunningTaskScore;
  }

  static boolean missingUsageData(SingularitySlaveUsage slaveUsage) {
    return !slaveUsage.getCpusTotal().isPresent() ||
        !slaveUsage.getMemoryMbTotal().isPresent() ||
        !slaveUsage.getDiskMbTotal().isPresent() ||
        slaveUsage.getLongRunningTasksUsage() == null ||
        !slaveUsage.getLongRunningTasksUsage().containsKey(ResourceUsageType.CPU_USED) ||
        !slaveUsage.getLongRunningTasksUsage().containsKey(ResourceUsageType.MEMORY_BYTES_USED) ||
        !slaveUsage.getLongRunningTasksUsage().containsKey(ResourceUsageType.DISK_BYTES_USED);
  }

  boolean isMissingUsageData() {
    return missingUsageData;
  }

  SingularitySlaveUsage getSlaveUsage() {
    return slaveUsage;
  }

  double getLongRunningCpusUsedScore() {
    return longRunningCpusUsedScore;
  }

  double getLongRunningMemUsedScore() {
    return longRunningMemUsedScore;
  }

  double getLongRunningDiskUsedScore() {
    return longRunningDiskUsedScore;
  }

  double getCpusFreeScore() {
    return cpusFreeScore;
  }

  double getMemFreeScore() {
    return memFreeScore;
  }

  double getDiskFreeScore() {
    return diskFreeScore;
  }

  double getDefaultLongRunningTaskScore() {
    return defaultLongRunningTaskScore;
  }

  @Override
  public String toString() {
    return "SingularitySlaveUsageWithCalculatedScores{" +
        "slaveUsage=" + slaveUsage +
        ", missingUsageData=" + missingUsageData +
        ", longRunningCpusUsedScore=" + longRunningCpusUsedScore +
        ", longRunningMemUsedScore=" + longRunningMemUsedScore +
        ", longRunningDiskUsedScore=" + longRunningDiskUsedScore +
        ", cpusFreeScore=" + cpusFreeScore +
        ", memFreeScore=" + memFreeScore +
        ", diskFreeScore=" + diskFreeScore +
        ", defaultLongRunningTaskScore=" + defaultLongRunningTaskScore +
        '}';
  }
}
