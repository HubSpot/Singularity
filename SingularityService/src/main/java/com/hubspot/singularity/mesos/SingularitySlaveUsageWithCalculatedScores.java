package com.hubspot.singularity.mesos;

import com.hubspot.singularity.MachineLoadMetric;
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularitySlaveUsage.ResourceUsageType;
import com.hubspot.singularity.SingularityUsageScoringStrategy;

class SingularitySlaveUsageWithCalculatedScores {
  private final SingularitySlaveUsage slaveUsage;
  private final MachineLoadMetric systemLoadMetric;
  private final MaxProbableUsage maxProbableTaskUsage;
  private boolean missingUsageData;
  private double longRunningCpusUsedScore;
  private double longRunningMemUsedScore;
  private double longRunningDiskUsedScore;
  private double cpusFreeScore;
  private double memFreeScore;
  private double diskFreeScore;

  private double estimatedAddedCpusUsage = 0;
  private double estimatedAddedMemoryBytesUsage = 0;
  private double estimatedAddedDiskBytesUsage = 0;

  private double estimatedAddedCpusReserved = 0;
  private double estimatedAddedMemoryBytesReserved = 0;
  private double estimatedAddedDiskBytesReserved = 0;

  private final double load5Threshold;
  private final double load1Threshold;

  public SingularitySlaveUsageWithCalculatedScores(SingularitySlaveUsage slaveUsage, SingularityUsageScoringStrategy scoringStrategy, MachineLoadMetric systemLoadMetric, MaxProbableUsage maxProbableTaskUsage, double load5Threshold, double load1Threshold) {
    this.slaveUsage = slaveUsage;
    this.systemLoadMetric = systemLoadMetric;
    this.maxProbableTaskUsage = maxProbableTaskUsage;
    if (missingUsageData(slaveUsage)) {
      this.missingUsageData = true;
      setScores(0, 0, 0, 0, 0, 0);
    } else {
      this.missingUsageData = false;
      setScores(scoringStrategy);
    }
    this.load5Threshold = load5Threshold;
    this.load1Threshold = load1Threshold;
  }

  public boolean isOverloaded() {
    return  (slaveUsage.getSystemLoad5Min() / slaveUsage.getSystemCpusTotal()) > load5Threshold || (slaveUsage.getSystemLoad1Min() / slaveUsage.getSystemCpusTotal()) > load1Threshold;
  }

  private void setScores(double longRunningCpusUsedScore,
                 double longRunningMemUsedScore,
                 double longRunningDiskUsedScore,
                 double cpusFreeScore,
                 double memFreeScore,
                 double diskFreeScore) {
    this.longRunningCpusUsedScore = longRunningCpusUsedScore;
    this.longRunningMemUsedScore = longRunningMemUsedScore;
    this.longRunningDiskUsedScore = longRunningDiskUsedScore;
    this.cpusFreeScore = cpusFreeScore;
    this.memFreeScore = memFreeScore;
    this.diskFreeScore = diskFreeScore;
  }

  private boolean missingUsageData(SingularitySlaveUsage slaveUsage) {
    return !slaveUsage.getCpusTotal().isPresent() ||
        !slaveUsage.getMemoryMbTotal().isPresent() ||
        !slaveUsage.getDiskMbTotal().isPresent() ||
        slaveUsage.getLongRunningTasksUsage() == null ||
        !slaveUsage.getLongRunningTasksUsage().containsKey(ResourceUsageType.CPU_USED) ||
        !slaveUsage.getLongRunningTasksUsage().containsKey(ResourceUsageType.MEMORY_BYTES_USED) ||
        !slaveUsage.getLongRunningTasksUsage().containsKey(ResourceUsageType.DISK_BYTES_USED);
  }

  void setScores(SingularityUsageScoringStrategy scoringStrategy) {
    double longRunningCpusUsedScore = (slaveUsage.getLongRunningTasksUsage().get(ResourceUsageType.CPU_USED).doubleValue() + estimatedAddedCpusUsage) / slaveUsage.getCpusTotal().get();
    double longRunningMemUsedScore = ((slaveUsage.getLongRunningTasksUsage().get(ResourceUsageType.MEMORY_BYTES_USED).longValue() + estimatedAddedMemoryBytesUsage) / slaveUsage.getMemoryBytesTotal().get());
    double longRunningDiskUsedScore = ((slaveUsage.getLongRunningTasksUsage().get(ResourceUsageType.DISK_BYTES_USED).longValue() + estimatedAddedDiskBytesUsage) / slaveUsage.getDiskBytesTotal().get());
    switch (scoringStrategy) {
      case SPREAD_TASK_USAGE:
        double cpusFreeScore = 1 - ((slaveUsage.getCpusReserved() + estimatedAddedCpusReserved) / slaveUsage.getCpusTotal().get());
        double memFreeScore = 1 - ((slaveUsage.getMemoryMbReserved() + (estimatedAddedMemoryBytesReserved / SingularitySlaveUsage.BYTES_PER_MEGABYTE)) / slaveUsage.getMemoryMbTotal().get());
        double diskFreeScore = 1 - ((slaveUsage.getDiskMbReserved() + (estimatedAddedDiskBytesReserved / SingularitySlaveUsage.BYTES_PER_MEGABYTE)) / slaveUsage.getDiskMbTotal().get());
        setScores(longRunningCpusUsedScore, longRunningMemUsedScore, longRunningDiskUsedScore, cpusFreeScore, memFreeScore, diskFreeScore);
        break;
      case PROBABLE_MAX_USAGE:
        double probableMaxCpuFreeScore = Math.max(
            0,
            1 - (getMaxProbableCpuWithEstimatedUsage() / slaveUsage.getSystemCpusTotal())
        );
        double probableMaxMemFreeScore = 1 - (getMaxProbableMemBytesWithEstimatedUsage() / slaveUsage.getSystemMemTotalBytes());
        double probableMaxDiskFreeScore = 1 - (getMaxProbableDiskBytesWithEstimatedUsage() / slaveUsage.getSlaveDiskTotal());
        setScores(longRunningCpusUsedScore, longRunningMemUsedScore, longRunningDiskUsedScore, probableMaxCpuFreeScore, probableMaxMemFreeScore, probableMaxDiskFreeScore);
        break;
      case SPREAD_SYSTEM_USAGE:
      default:
        double systemCpuFreeScore = Math.max(0, 1 - ((getSystemLoadMetric() + estimatedAddedCpusUsage) / slaveUsage.getSystemCpusTotal()));
        double systemMemFreeScore = 1 - (slaveUsage.getSystemMemTotalBytes() - slaveUsage.getSystemMemFreeBytes() + estimatedAddedMemoryBytesUsage) / slaveUsage.getSystemMemTotalBytes();
        double systemDiskFreeScore = 1 - ((slaveUsage.getSlaveDiskUsed() + estimatedAddedDiskBytesUsage) / slaveUsage.getSlaveDiskTotal());
        setScores(longRunningCpusUsedScore, longRunningMemUsedScore, longRunningDiskUsedScore, systemCpuFreeScore, systemMemFreeScore, systemDiskFreeScore);
    }
  }

  private double getMaxProbableCpuWithEstimatedUsage() {
    return Math.max(getSystemLoadMetric(), maxProbableTaskUsage.getCpu()) + estimatedAddedCpusUsage;
  }

  private double getMaxProbableMemBytesWithEstimatedUsage() {
    return Math.max(slaveUsage.getSystemMemTotalBytes() - slaveUsage.getSystemMemFreeBytes(), maxProbableTaskUsage.getMemBytes()) + estimatedAddedMemoryBytesUsage;
  }

  private double getMaxProbableDiskBytesWithEstimatedUsage() {
    return Math.max(slaveUsage.getSlaveDiskUsed(), maxProbableTaskUsage.getDiskBytes()) + estimatedAddedDiskBytesUsage;
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

  void addEstimatedCpuUsage(double estimatedAddedCpus) {
    this.estimatedAddedCpusUsage += estimatedAddedCpus;
  }

  void addEstimatedMemoryBytesUsage(double estimatedAddedMemoryBytes) {
    this.estimatedAddedMemoryBytesUsage += estimatedAddedMemoryBytes;
  }

  void addEstimatedDiskBytesUsage(double estimatedAddedDiskBytes) {
    this.estimatedAddedDiskBytesUsage = estimatedAddedDiskBytes;
  }

  void addEstimatedCpuReserved(double estimatedAddedCpus) {
    this.estimatedAddedCpusReserved += estimatedAddedCpus;
  }

  void addEstimatedMemoryReserved(double estimatedAddedMemoryBytes) {
    this.estimatedAddedMemoryBytesReserved += estimatedAddedMemoryBytes;
  }

  void addEstimatedDiskReserved(double estimatedAddedDiskBytes) {
    this.estimatedAddedDiskBytesReserved = estimatedAddedDiskBytes;
  }

  private double getSystemLoadMetric() {
    switch (systemLoadMetric) {
      case LOAD_1:
        return slaveUsage.getSystemLoad1Min();
      case LOAD_15:
        return slaveUsage.getSystemLoad15Min();
      case LOAD_5:
      default:
        return slaveUsage.getSystemLoad5Min();
    }
  }

  static class MaxProbableUsage {
    private final double cpu;
    private final double memBytes;
    private final double diskBytes;

    public MaxProbableUsage(double cpu, double memBytes, double diskBytes) {
      this.cpu = cpu;
      this.memBytes = memBytes;
      this.diskBytes = diskBytes;
    }

    public double getCpu() {
      return cpu;
    }

    public double getMemBytes() {
      return memBytes;
    }

    public double getDiskBytes() {
      return diskBytes;
    }
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
        ", estimatedAddedCpusUsage=" + estimatedAddedCpusUsage +
        ", estimatedAddedMemoryBytesUsage=" + estimatedAddedMemoryBytesUsage +
        ", estimatedAddedDiskBytesUsage=" + estimatedAddedDiskBytesUsage +
        ", estimatedAddedCpusReserved=" + estimatedAddedCpusReserved +
        ", estimatedAddedMemoryBytesReserved=" + estimatedAddedMemoryBytesReserved +
        ", estimatedAddedDiskBytesReserved=" + estimatedAddedDiskBytesReserved +
        '}';
  }
}
