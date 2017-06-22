package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RequestUtilization {
  private final String requestId;
  private final String deployId;
  private long memBytesTotal = 0;
  private double cpuTotal = 0;
  private int numTasks = 0;

  @JsonCreator
  public RequestUtilization(@JsonProperty("requestId") String requestId,
                            @JsonProperty("deployId") String deployId) {
    this.requestId = requestId;
    this.deployId = deployId;
  }

  public RequestUtilization addMemBytes(long memBytes) {
    this.memBytesTotal += memBytes;
    return this;
  }

  public RequestUtilization addCpu(double cpu) {
    this.cpuTotal += cpu;
    return this;
  }

  public RequestUtilization incrementTaskCount() {
    this.numTasks++;
    return this;
  }

  public long getMemBytesTotal() {
    return memBytesTotal;
  }

  public double getCpuTotal() {
    return cpuTotal;
  }

  public int getNumTasks() {
    return numTasks;
  }

  @JsonIgnore
  public double getAvgMemBytesUsed() {
    return memBytesTotal / (double) numTasks;
  }

  @JsonIgnore
  public double getAvgCpuUsed() {
    return cpuTotal / (double) numTasks;
  }

  public String getDeployId() {
    return deployId;
  }

  public String getRequestId() {
    return requestId;
  }

  @Override
  public String toString() {
    return "RequestUtilization{" +
        "requestId=" + requestId +
        ", deployId=" + deployId +
        ", memBytesTotal=" + memBytesTotal +
        ", cpuTotal=" + cpuTotal +
        ", numTasks=" + numTasks +
        '}';
  }
}
