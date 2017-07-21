package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityClusterUtilization {
  private final List<RequestUtilization> requestUtilizations;

  private final int numRequestsWithUnderUtilizedCpu;
  private final int numRequestsWithOverUtilizedCpu;
  private final int numRequestsWithUnderUtilizedMemBytes;
  private final int numRequestsWithUnderUtilizedDiskBytes;

  private final double totalUnderUtilizedCpu;
  private final double totalOverUtilizedCpu;
  private final long totalUnderUtilizedMemBytes;
  private final long totalUnderUtilizedDiskBytes;

  private final double avgUnderUtilizedCpu;
  private final double avgOverUtilizedCpu;
  private final long avgUnderUtilizedMemBytes;
  private final long avgUnderUtilizedDiskBytes;

  private final double maxUnderUtilizedCpu;
  private final double maxOverUtilizedCpu;
  private final long maxUnderUtilizedMemBytes;
  private final long maxUnderUtilizedDiskBytes;

  private final String maxUnderUtilizedCpuRequestId;
  private final String maxOverUtilizedCpuRequestId;
  private final String maxUnderUtilizedMemBytesRequestId;
  private final String maxUnderUtilizedDiskBytesRequestId;

  private final double minUnderUtilizedCpu;
  private final double minOverUtilizedCpu;
  private final long minUnderUtilizedMemBytes;
  private final long minUnderUtilizedDiskBytes;

  private final long totalMemBytesUsed;
  private final long totalMemBytesAvailable;

  private final long totalDiskBytesUsed;
  private final long totalDiskBytesAvailable;

  private final double totalCpuUsed;
  private final double totalCpuAvailable;

  private final long timestamp;

  @JsonCreator
  public SingularityClusterUtilization(@JsonProperty("requestUtilizations") List<RequestUtilization> requestUtilizations,
                                       @JsonProperty("numRequestsWithUnderUtilizedCpu") int numRequestsWithUnderUtilizedCpu,
                                       @JsonProperty("numRequestsWithOverUtilizedCpu") int numRequestsWithOverUtilizedCpu,
                                       @JsonProperty("numRequestsWithUnderUtilizedMemBytes") int numRequestsWithUnderUtilizedMemBytes,
                                       @JsonProperty("numRequestsWithUnderUtilizedDiskBytes") int numRequestsWithUnderUtilizedDiskBytes,
                                       @JsonProperty("totalUnderUtilizedCpu") double totalUnderUtilizedCpu,
                                       @JsonProperty("totalOverUtilizedCpu") double totalOverUtilizedCpu,
                                       @JsonProperty("totalUnderUtilizedMemBytes") long totalUnderUtilizedMemBytes,
                                       @JsonProperty("totalUnderUtilizedDiskBytes") long totalUnderUtilizedDiskBytes,
                                       @JsonProperty("avgUnderUtilizedCpu") double avgUnderUtilizedCpu,
                                       @JsonProperty("avgOverUtilizedCpu") double avgOverUtilizedCpu,
                                       @JsonProperty("avgUnderUtilizedMemBytes") long avgUnderUtilizedMemBytes,
                                       @JsonProperty("avgUnderUtilizedDiskBytes") long avgUnderUtilizedDiskBytes,
                                       @JsonProperty("maxUnderUtilizedCpu") double maxUnderUtilizedCpu,
                                       @JsonProperty("maxOverUtilizedCpu") double maxOverUtilizedCpu,
                                       @JsonProperty("maxUnderUtilizedMemBytes") long maxUnderUtilizedMemBytes,
                                       @JsonProperty("maxUnderUtilizedDiskBytes") long maxUnderUtilizedDiskBytes,
                                       @JsonProperty("maxUnderUtilizedCpuRequestId") String maxUnderUtilizedCpuRequestId,
                                       @JsonProperty("maxOverUtilizedCpuRequestId") String maxOverUtilizedCpuRequestId,
                                       @JsonProperty("maxUnderUtilizedMemBytesRequestId") String maxUnderUtilizedMemBytesRequestId,
                                       @JsonProperty("maxUnderUtilizedDiskBytesRequestId") String maxUnderUtilizedDiskBytesRequestId,
                                       @JsonProperty("minUnderUtilizedCpu") double minUnderUtilizedCpu,
                                       @JsonProperty("minOverUtilizedCpu") double minOverUtilizedCpu,
                                       @JsonProperty("minUnderUtilizedMemBytes") long minUnderUtilizedMemBytes,
                                       @JsonProperty("minUnderUtilizedDiskBytes") long minUnderUtilizedDiskBytes,
                                       @JsonProperty("totalMemBytesUsed") long totalMemBytesUsed,
                                       @JsonProperty("totalMemBytesAvailable") long totalMemBytesAvailable,
                                       @JsonProperty("totalDiskBytesUsed") long totalDiskBytesUsed,
                                       @JsonProperty("totalDiskBytesAvailable") long totalDiskBytesAvailable,
                                       @JsonProperty("totalCpuUsed") double totalCpuUsed,
                                       @JsonProperty("totalCpuAvailable") double totalCpuAvailable,
                                       @JsonProperty("timestamp") long timestamp) {
    this.requestUtilizations = requestUtilizations;
    this.numRequestsWithUnderUtilizedCpu = numRequestsWithUnderUtilizedCpu;
    this.numRequestsWithOverUtilizedCpu = numRequestsWithOverUtilizedCpu;
    this.numRequestsWithUnderUtilizedMemBytes = numRequestsWithUnderUtilizedMemBytes;
    this.numRequestsWithUnderUtilizedDiskBytes = numRequestsWithUnderUtilizedDiskBytes;
    this.totalUnderUtilizedCpu = totalUnderUtilizedCpu;
    this.totalOverUtilizedCpu = totalOverUtilizedCpu;
    this.totalUnderUtilizedMemBytes = totalUnderUtilizedMemBytes;
    this.totalUnderUtilizedDiskBytes = totalUnderUtilizedDiskBytes;
    this.avgUnderUtilizedCpu = avgUnderUtilizedCpu;
    this.avgOverUtilizedCpu = avgOverUtilizedCpu;
    this.avgUnderUtilizedMemBytes = avgUnderUtilizedMemBytes;
    this.avgUnderUtilizedDiskBytes = avgUnderUtilizedDiskBytes;
    this.maxUnderUtilizedCpu = maxUnderUtilizedCpu;
    this.maxOverUtilizedCpu = maxOverUtilizedCpu;
    this.maxUnderUtilizedMemBytes = maxUnderUtilizedMemBytes;
    this.maxUnderUtilizedDiskBytes = maxUnderUtilizedDiskBytes;
    this.maxUnderUtilizedCpuRequestId = maxUnderUtilizedCpuRequestId;
    this.maxOverUtilizedCpuRequestId = maxOverUtilizedCpuRequestId;
    this.maxUnderUtilizedMemBytesRequestId = maxUnderUtilizedMemBytesRequestId;
    this.maxUnderUtilizedDiskBytesRequestId = maxUnderUtilizedDiskBytesRequestId;
    this.minUnderUtilizedCpu = minUnderUtilizedCpu;
    this.minOverUtilizedCpu = minOverUtilizedCpu;
    this.minUnderUtilizedMemBytes = minUnderUtilizedMemBytes;
    this.minUnderUtilizedDiskBytes = minUnderUtilizedDiskBytes;
    this.totalMemBytesUsed = totalMemBytesUsed;
    this.totalMemBytesAvailable = totalMemBytesAvailable;
    this.totalDiskBytesUsed = totalDiskBytesUsed;
    this.totalDiskBytesAvailable = totalDiskBytesAvailable;
    this.totalCpuUsed = totalCpuUsed;
    this.totalCpuAvailable = totalCpuAvailable;
    this.timestamp = timestamp;
  }

  public List<RequestUtilization> getRequestUtilizations() {
    return requestUtilizations;
  }

  public int getNumRequestsWithUnderUtilizedCpu() {
    return numRequestsWithUnderUtilizedCpu;
  }

  public int getNumRequestsWithOverUtilizedCpu() {
    return numRequestsWithOverUtilizedCpu;
  }

  public int getNumRequestsWithUnderUtilizedMemBytes() {
    return numRequestsWithUnderUtilizedMemBytes;
  }

  public int getNumRequestsWithUnderUtilizedDiskBytes() {
    return numRequestsWithUnderUtilizedDiskBytes;
  }

  public double getTotalUnderUtilizedCpu() {
    return totalUnderUtilizedCpu;
  }

  public double getTotalOverUtilizedCpu() {
    return totalOverUtilizedCpu;
  }

  public long getTotalUnderUtilizedMemBytes() {
    return totalUnderUtilizedMemBytes;
  }

  public long getTotalUnderUtilizedDiskBytes() {
    return totalUnderUtilizedDiskBytes;
  }

  public double getAvgUnderUtilizedCpu() {
    return avgUnderUtilizedCpu;
  }

  public double getAvgOverUtilizedCpu() {
    return avgOverUtilizedCpu;
  }

  public long getAvgUnderUtilizedMemBytes() {
    return avgUnderUtilizedMemBytes;
  }

  public long getAvgUnderUtilizedDiskBytes() {
    return avgUnderUtilizedDiskBytes;
  }

  public double getMaxUnderUtilizedCpu() {
    return maxUnderUtilizedCpu;
  }

  public double getMaxOverUtilizedCpu() {
    return maxOverUtilizedCpu;
  }

  public long getMaxUnderUtilizedMemBytes() {
    return maxUnderUtilizedMemBytes;
  }

  public long getMaxUnderUtilizedDiskBytes() {
    return maxUnderUtilizedDiskBytes;
  }

  public String getMaxUnderUtilizedCpuRequestId() {
    return maxUnderUtilizedCpuRequestId;
  }

  public String getMaxOverUtilizedCpuRequestId() {
    return maxOverUtilizedCpuRequestId;
  }

  public String getMaxUnderUtilizedMemBytesRequestId() {
    return maxUnderUtilizedMemBytesRequestId;
  }

  public String getMaxUnderUtilizedDiskBytesRequestId() {
    return maxUnderUtilizedDiskBytesRequestId;
  }

  public double getMinUnderUtilizedCpu() {
    return minUnderUtilizedCpu;
  }

  public double getMinOverUtilizedCpu() {
    return minOverUtilizedCpu;
  }

  public long getMinUnderUtilizedMemBytes() {
    return minUnderUtilizedMemBytes;
  }

  public long getMinUnderUtilizedDiskBytes() {
    return minUnderUtilizedDiskBytes;
  }

  public long getTotalMemBytesUsed() {
    return totalMemBytesUsed;
  }

  public long getTotalMemBytesAvailable() {
    return totalMemBytesAvailable;
  }

  public long getTotalDiskBytesUsed() {
    return totalDiskBytesUsed;
  }

  public long getTotalDiskBytesAvailable() {
    return totalDiskBytesAvailable;
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
        ", requestUtilizations=" + requestUtilizations +
        ", numRequestsWithUnderUtilizedCpu=" + numRequestsWithUnderUtilizedCpu +
        ", numRequestsWithOverUtilizedCpu=" + numRequestsWithOverUtilizedCpu +
        ", numRequestsWithUnderUtilizedMemBytes=" + numRequestsWithUnderUtilizedMemBytes +
        ", totalUnderUtilizedCpu=" + totalUnderUtilizedCpu +
        ", totalOverUtilizedCpu=" + totalOverUtilizedCpu +
        ", totalUnderUtilizedMemBytes=" + totalUnderUtilizedMemBytes +
        ", avgUnderUtilizedCpu=" + avgUnderUtilizedCpu +
        ", avgOverUtilizedCpu=" + avgOverUtilizedCpu +
        ", avgUnderUtilizedMemBytes=" + avgUnderUtilizedMemBytes +
        ", maxUnderUtilizedCpu=" + maxUnderUtilizedCpu +
        ", maxOverUtilizedCpu=" + maxOverUtilizedCpu +
        ", maxUnderUtilizedMemBytes=" + maxUnderUtilizedMemBytes +
        ", maxUnderUtilizedCpuRequestId=" + maxUnderUtilizedCpuRequestId +
        ", maxOverUtilizedCpuRequestId=" + maxOverUtilizedCpuRequestId +
        ", maxUnderUtilizedMemBytesRequestId=" + maxUnderUtilizedMemBytesRequestId +
        ", minUnderUtilizedCpu=" + minUnderUtilizedCpu +
        ", minOverUtilizedCpu=" + minOverUtilizedCpu +
        ", minUnderUtilizedMemBytes=" + minUnderUtilizedMemBytes +
        ", totalMemBytesUsed=" + totalMemBytesUsed +
        ", totalMemBytesAvailable=" + totalMemBytesAvailable +
        ", totalCpuUsed=" + totalCpuUsed +
        ", totalCpuAvailable=" + totalCpuAvailable +
        ", timestamp=" + timestamp +
        "]";
  }
}
