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
  private long minMemBytesUsed = Long.MAX_VALUE;
  private double maxCpuUsed = 0;
  private double minCpuUsed = Double.MAX_VALUE;
  private long maxDiskBytesUsed = 0;
  private long minDiskBytesUsed = 0;

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

  public int getNumTasks() {
    return numTasks;
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

  @Override
  public String toString() {
    return "RequestUtilization{" +
        "requestId=" + requestId +
        ", deployId=" + deployId +
        ", memBytesUsed=" + memBytesUsed +
        ", memBytesReserved=" + memBytesReserved +
        ", cpuUsed=" + cpuUsed +
        ", cpuReserved=" + cpuReserved +
        ", numTasks=" + numTasks +
        '}';
  }
}
