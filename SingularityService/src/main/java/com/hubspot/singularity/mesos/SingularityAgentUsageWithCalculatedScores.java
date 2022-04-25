package com.hubspot.singularity.mesos;

import com.hubspot.singularity.MachineLoadMetric;
import com.hubspot.singularity.SingularityAgentUsage;

class SingularityAgentUsageWithCalculatedScores {
  private final SingularityAgentUsage agentUsage;
  private final MachineLoadMetric systemLoadMetric;
  private final MaxProbableUsage maxProbableTaskUsage;
  private boolean missingUsageData;
  private double cpusAllocatedScore;
  private double memAllocatedScore;
  private double diskAllocatedScore;
  private double cpusInUseScore;
  private double memInUseScore;
  private double diskInUseScore;

  private double estimatedAddedCpusUsage = 0;
  private double estimatedAddedMemoryBytesUsage = 0;
  private double estimatedAddedDiskBytesUsage = 0;

  private double estimatedAddedCpusReserved = 0;
  private double estimatedAddedMemoryBytesReserved = 0;
  private double estimatedAddedDiskBytesReserved = 0;

  private final double load5Threshold;
  private final double load1Threshold;

  private final long timestamp;

  SingularityAgentUsageWithCalculatedScores(
    SingularityAgentUsage agentUsage,
    MachineLoadMetric systemLoadMetric,
    MaxProbableUsage maxProbableTaskUsage,
    double load5Threshold,
    double load1Threshold,
    long timestamp
  ) {
    this.agentUsage = agentUsage;
    this.systemLoadMetric = systemLoadMetric;
    this.maxProbableTaskUsage = maxProbableTaskUsage;
    if (missingUsageData(agentUsage)) {
      this.missingUsageData = true;
      setScores(0, 0, 0, 0, 0, 0);
    } else {
      this.missingUsageData = false;
      recalculateScores();
    }
    this.load5Threshold = load5Threshold;
    this.load1Threshold = load1Threshold;
    this.timestamp = timestamp;
  }

  boolean isCpuOverloaded(double estimatedNumCpusToAdd) {
    return (
      (
        (
          agentUsage.getSystemLoad5Min() + estimatedAddedCpusUsage + estimatedNumCpusToAdd
        ) /
        agentUsage.getSystemCpusTotal()
      ) >
      load5Threshold ||
      (
        (
          agentUsage.getSystemLoad1Min() + estimatedAddedCpusUsage + estimatedNumCpusToAdd
        ) /
        agentUsage.getSystemCpusTotal()
      ) >
      load1Threshold
    );
  }

  private void setScores(
    double cpusAllocatedScore,
    double memAllocatedScore,
    double diskAllocatedScore,
    double cpusInUseScore,
    double memInUseScore,
    double diskInUseScore
  ) {
    this.cpusAllocatedScore = cpusAllocatedScore;
    this.memAllocatedScore = memAllocatedScore;
    this.diskAllocatedScore = diskAllocatedScore;
    this.cpusInUseScore = cpusInUseScore;
    this.memInUseScore = memInUseScore;
    this.diskInUseScore = diskInUseScore;
  }

  private boolean missingUsageData(SingularityAgentUsage agentUsage) {
    return (
      !agentUsage.getCpusTotal().isPresent() ||
      !agentUsage.getMemoryMbTotal().isPresent() ||
      !agentUsage.getDiskMbTotal().isPresent()
    );
  }

  void recalculateScores() {
    setScores(
      (agentUsage.getCpusReserved() + estimatedAddedCpusUsage) /
      agentUsage.getCpusTotal().get(),
      (
        (agentUsage.getMemoryMbReserved() * SingularityAgentUsage.BYTES_PER_MEGABYTE) +
        estimatedAddedMemoryBytesUsage
      ) /
      agentUsage.getMemoryBytesTotal().get(),
      (
        (agentUsage.getDiskMbReserved() * SingularityAgentUsage.BYTES_PER_MEGABYTE) +
        estimatedAddedDiskBytesUsage
      ) /
      agentUsage.getDiskBytesTotal().get(),
      Math.max(
        0,
        1 - (getMaxProbableCpuWithEstimatedUsage() / agentUsage.getSystemCpusTotal())
      ),
      1 -
      (getMaxProbableMemBytesWithEstimatedUsage() / agentUsage.getSystemMemTotalBytes()),
      1 - (getMaxProbableDiskBytesWithEstimatedUsage() / agentUsage.getDiskTotal())
    );
  }

  private double getMaxProbableCpuWithEstimatedUsage() {
    return (
      Math.max(getSystemLoadMetric(), maxProbableTaskUsage.getCpu()) +
      estimatedAddedCpusUsage
    );
  }

  private double getMaxProbableMemBytesWithEstimatedUsage() {
    return (
      Math.max(
        agentUsage.getSystemMemTotalBytes() - agentUsage.getSystemMemFreeBytes(),
        maxProbableTaskUsage.getMemBytes()
      ) +
      estimatedAddedMemoryBytesUsage
    );
  }

  private double getMaxProbableDiskBytesWithEstimatedUsage() {
    return (
      Math.max(agentUsage.getDiskUsed(), maxProbableTaskUsage.getDiskBytes()) +
      estimatedAddedDiskBytesUsage
    );
  }

  boolean isMissingUsageData() {
    return missingUsageData;
  }

  SingularityAgentUsage getAgentUsage() {
    return agentUsage;
  }

  double getCpusAllocatedScore() {
    return cpusAllocatedScore;
  }

  double getMemAllocatedScore() {
    return memAllocatedScore;
  }

  double getDiskAllocatedScore() {
    return diskAllocatedScore;
  }

  double getCpusInUseScore() {
    return cpusInUseScore;
  }

  double getMemInUseScore() {
    return memInUseScore;
  }

  double getDiskInUseScore() {
    return diskInUseScore;
  }

  public double getEstimatedAddedCpusUsage() {
    return estimatedAddedCpusUsage;
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
        return agentUsage.getSystemLoad1Min();
      case LOAD_15:
        return agentUsage.getSystemLoad15Min();
      case LOAD_5:
      default:
        return agentUsage.getSystemLoad5Min();
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
    return (
      "SingularityAgentUsageWithCalculatedScores{" +
      "slaveUsage=" +
      agentUsage +
      ", missingUsageData=" +
      missingUsageData +
      ", cpusAllocatedScore=" +
      cpusAllocatedScore +
      ", memAllocatedScore=" +
      memAllocatedScore +
      ", diskAllocatedScore=" +
      diskAllocatedScore +
      ", cpusInUseScore=" +
      cpusInUseScore +
      ", memInUseScore=" +
      memInUseScore +
      ", diskInUseScore=" +
      diskInUseScore +
      ", estimatedAddedCpusUsage=" +
      estimatedAddedCpusUsage +
      ", estimatedAddedMemoryBytesUsage=" +
      estimatedAddedMemoryBytesUsage +
      ", estimatedAddedDiskBytesUsage=" +
      estimatedAddedDiskBytesUsage +
      ", estimatedAddedCpusReserved=" +
      estimatedAddedCpusReserved +
      ", estimatedAddedMemoryBytesReserved=" +
      estimatedAddedMemoryBytesReserved +
      ", estimatedAddedDiskBytesReserved=" +
      estimatedAddedDiskBytesReserved +
      '}'
    );
  }
}
