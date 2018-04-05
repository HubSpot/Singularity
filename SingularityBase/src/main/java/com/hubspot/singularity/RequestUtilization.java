package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RequestUtilization {
  private final String requestId;
  private final String deployId;

  private long memBytesUsed = 0;
  private long memBytesReserved = 0;
  private double cpuUsed = 0;
  private double cpuReserved = 0;
  private long diskBytesUsed = 0;
  private long diskBytesReserved = 0;
  private int numTasks = 0;

  private long maxMemBytesUsed = 0;
  private double maxMemTimestamp = 0;
  private long minMemBytesUsed = Long.MAX_VALUE;
  private double minMemTimestamp = 0;
  private double maxCpuUsed = 0;
  private double maxCpusTimestamp = 0;
  private double minCpuUsed = Double.MAX_VALUE;
  private double minCpusTimestamp = 0;
  private long maxDiskBytesUsed = 0;
  private double maxDiskTimestamp = 0;
  private long minDiskBytesUsed = Long.MAX_VALUE;
  private double minDiskTimestamp = 0;

  private double cpuBurstRating = 0;

  private double percentCpuTimeThrottled = 0;
  private double maxPercentCpuTimeThrottled = 0;
  private double maxCpuThrottledTimestamp = 0;
  private double minPercentCpuTimeThrottled = Double.MAX_VALUE;
  private double minCpuThrottledTimestamp = 0;

  @JsonCreator
  public RequestUtilization(@JsonProperty("requestId") String requestId,
                            @JsonProperty("deployId") String deployId) {
    this.requestId = requestId;
    this.deployId = deployId;
  }

  public RequestUtilization addMemBytesUsed(long memBytes) {
    this.memBytesUsed += memBytes;
    return this;
  }

  public RequestUtilization addMemBytesReserved(long memBytes) {
    this.memBytesReserved += memBytes;
    return this;
  }

  // This is a running total, not the current usage
  public RequestUtilization addCpuUsed(double cpu) {
    this.cpuUsed += cpu;
    return this;
  }

  public RequestUtilization addCpuReserved(double cpu) {
    this.cpuReserved += cpu;
    return this;
  }

  public RequestUtilization addDiskBytesUsed(long diskBytes) {
    this.diskBytesUsed += diskBytes;
    return this;
  }

  public RequestUtilization addDiskBytesReserved(long diskBytes) {
    this.diskBytesReserved += diskBytes;
    return this;
  }

  public RequestUtilization addPercentCpuTimeThrottled(double percentCpuTimeThrottled) {
    this.percentCpuTimeThrottled += percentCpuTimeThrottled;
    return this;
  }

  public RequestUtilization incrementTaskCount() {
    this.numTasks++;
    return this;
  }

  public long getMemBytesUsed() {
    return memBytesUsed;
  }

  public long getMemBytesReserved() {
    return memBytesReserved;
  }

  public double getCpuUsed() {
    return cpuUsed;
  }

  public double getCpuReserved() {
    return cpuReserved;
  }

  public long getDiskBytesUsed() {
    return diskBytesUsed;
  }

  public long getDiskBytesReserved() {
    return diskBytesReserved;
  }

  public double getPercentCpuTimeThrottled() {
    return percentCpuTimeThrottled;
  }

  public int getNumTasks() {
    return numTasks;
  }

  // 0 -> 1, where 0 is never over-utilized, or only short bursts and 1 is consistently overutilized
  public double getCpuBurstRating() {
    return cpuBurstRating;
  }

  @JsonIgnore
  public double getAvgMemBytesUsed() {
    return memBytesUsed / (double) numTasks;
  }

  @JsonIgnore
  public double getAvgCpuUsed() {
    return cpuUsed / (double) numTasks;
  }

  @JsonIgnore
  public double getAvgDiskBytesUsed() {
    return diskBytesUsed / (double) numTasks;
  }

  @JsonIgnore
  public double getAvgPercentCpuTimeThrottled() {
    return percentCpuTimeThrottled / numTasks;
  }

  public String getDeployId() {
    return deployId;
  }

  public String getRequestId() {
    return requestId;
  }

  public long getMaxMemBytesUsed() {
    return maxMemBytesUsed;
  }

  public RequestUtilization setMaxMemBytesUsed(long maxMemBytesUsed) {
    this.maxMemBytesUsed = maxMemBytesUsed;
    return this;
  }

  public double getMaxCpuUsed() {
    return maxCpuUsed;
  }

  public RequestUtilization setMaxCpuUsed(double maxCpuUsed) {
    this.maxCpuUsed = maxCpuUsed;
    return this;
  }

  public long getMaxDiskBytesUsed() {
    return maxDiskBytesUsed;
  }

  public RequestUtilization setMaxDiskBytesUsed(long maxDiskBytesUsed) {
    this.maxDiskBytesUsed = maxDiskBytesUsed;
    return this;
  }

  public long getMinMemBytesUsed() {
    return minMemBytesUsed;
  }

  public RequestUtilization setMinMemBytesUsed(long minMemBytesUsed) {
    this.minMemBytesUsed = minMemBytesUsed;
    return this;
  }

  public double getMinCpuUsed() {
    return minCpuUsed;
  }

  public RequestUtilization setMinCpuUsed(double minCpuUsed) {
    this.minCpuUsed = minCpuUsed;
    return this;
  }

  public long getMinDiskBytesUsed() {
    return minDiskBytesUsed;
  }

  public RequestUtilization setMinDiskBytesUsed(long minDiskBytesUsed) {
    this.minDiskBytesUsed = minDiskBytesUsed;
    return this;
  }

  public RequestUtilization setCpuBurstRating(double cpuBurstRating) {
    this.cpuBurstRating = cpuBurstRating;
    return this;
  }

  public double getMaxPercentCpuTimeThrottled() {
    return maxPercentCpuTimeThrottled;
  }

  public RequestUtilization setMaxPercentCpuTimeThrottled(double maxPercentCpuTimeThrottled) {
    this.maxPercentCpuTimeThrottled = maxPercentCpuTimeThrottled;
    return this;
  }

  public double getMinPercentCpuTimeThrottled() {
    return minPercentCpuTimeThrottled;
  }

  public RequestUtilization setMinPercentCpuTimeThrottled(double minPercentCpuTimeThrottled) {
    this.minPercentCpuTimeThrottled = minPercentCpuTimeThrottled;
    return this;
  }

  public double getMaxMemTimestamp() {
    return maxMemTimestamp;
  }

  public RequestUtilization setMaxMemTimestamp(double maxMemTimestamp) {
    this.maxMemTimestamp = maxMemTimestamp;
    return this;
  }

  public double getMinMemTimestamp() {
    return minMemTimestamp;
  }

  public RequestUtilization setMinMemTimestamp(double minMemTimestamp) {
    this.minMemTimestamp = minMemTimestamp;
    return this;
  }

  public double getMaxCpusTimestamp() {
    return maxCpusTimestamp;
  }

  public RequestUtilization setMaxCpusTimestamp(double maxCpusTimestamp) {
    this.maxCpusTimestamp = maxCpusTimestamp;
    return this;
  }

  public double getMinCpusTimestamp() {
    return minCpusTimestamp;
  }

  public RequestUtilization setMinCpusTimestamp(double minCpusTimestamp) {
    this.minCpusTimestamp = minCpusTimestamp;
    return this;
  }

  public double getMaxDiskTimestamp() {
    return maxDiskTimestamp;
  }

  public RequestUtilization setMaxDiskTimestamp(double maxDiskTimestamp) {
    this.maxDiskTimestamp = maxDiskTimestamp;
    return this;
  }

  public double getMinDiskTimestamp() {
    return minDiskTimestamp;
  }

  public RequestUtilization setMinDiskTimestamp(double minDiskTimestamp) {
    this.minDiskTimestamp = minDiskTimestamp;
    return this;
  }

  public double getMaxCpuThrottledTimestamp() {
    return maxCpuThrottledTimestamp;
  }

  public RequestUtilization setMaxCpuThrottledTimestamp(double maxCpuThrottledTimestamp) {
    this.maxCpuThrottledTimestamp = maxCpuThrottledTimestamp;
    return this;
  }

  public double getMinCpuThrottledTimestamp() {
    return minCpuThrottledTimestamp;
  }

  public RequestUtilization setMinCpuThrottledTimestamp(double minCpuThrottledTimestamp) {
    this.minCpuThrottledTimestamp = minCpuThrottledTimestamp;
    return this;
  }

  @Override
  public String toString() {
    return "RequestUtilization{" +
        "requestId='" + requestId + '\'' +
        ", deployId='" + deployId + '\'' +
        ", memBytesUsed=" + memBytesUsed +
        ", memBytesReserved=" + memBytesReserved +
        ", cpuUsed=" + cpuUsed +
        ", cpuReserved=" + cpuReserved +
        ", diskBytesUsed=" + diskBytesUsed +
        ", diskBytesReserved=" + diskBytesReserved +
        ", numTasks=" + numTasks +
        ", maxMemBytesUsed=" + maxMemBytesUsed +
        ", minMemBytesUsed=" + minMemBytesUsed +
        ", maxCpuUsed=" + maxCpuUsed +
        ", minCpuUsed=" + minCpuUsed +
        ", maxDiskBytesUsed=" + maxDiskBytesUsed +
        ", minDiskBytesUsed=" + minDiskBytesUsed +
        ", cpuBurstRating=" + cpuBurstRating +
        ", percentCpuTimeThrottled=" + percentCpuTimeThrottled +
        ", maxPercentCpuTimeThrottled=" + maxPercentCpuTimeThrottled +
        ", minPercentCpuTimeThrottled=" + minPercentCpuTimeThrottled +
        '}';
  }
}
