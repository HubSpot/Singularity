package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityClusterUtilization {
  private long totalMemBytesUsed;
  private long totalMemBytesAvailable;
  private double totalCpuUsed;
  private double totalCpuAvailable;
  private final long timestamp;

  @JsonCreator
  public SingularityClusterUtilization(@JsonProperty("totalMemBytesUsed") long totalMemBytesUsed,
                                       @JsonProperty("totalMemBytesAvailable") long totalMemBytesAvailable,
                                       @JsonProperty("totalCpuUsed") double totalCpuUsed,
                                       @JsonProperty("totalCpuAvailable") double totalCpuAvailable,
                                       @JsonProperty("timestamp") long timestamp) {
    this.totalMemBytesUsed = totalMemBytesUsed;
    this.totalMemBytesAvailable = totalMemBytesAvailable;
    this.totalCpuUsed = totalCpuUsed;
    this.totalCpuAvailable = totalCpuAvailable;
    this.timestamp = timestamp;
  }

  public long getTotalMemBytesUsed() {
    return totalMemBytesUsed;
  }

  public long getTotalMemBytesAvailable() {
    return totalMemBytesAvailable;
  }

  public double getTotalCpuUsed() {
    return totalCpuUsed;
  }

  public double getTotalCpuAvailable() {
    return totalCpuAvailable;
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return "SingularityClusterUtilization [" +
        "totalMemBytesUsed=" + totalMemBytesUsed +
        ", totalMemBytesAvailable=" + totalMemBytesAvailable +
        ", totalCpuUsed=" + totalCpuUsed +
        ", totalCpuAvailable=" + totalCpuAvailable +
        ", timestamp=" + timestamp +
        "]";

  }
}
