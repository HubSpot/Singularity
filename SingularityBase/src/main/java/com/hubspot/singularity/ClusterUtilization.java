package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ClusterUtilization {
  private final List<RequestUtilization> requestUtilizations;

  private final int numRequestsWithUnderUtilizedCpu;
  private final int numRequestsWithOverUtilizedCpu;
  private final int numRequestsWithUnderUtilizedMemBytes;

  private final double totalUnderUtilizedCpu;
  private final double totalOverUtilizedCpu;
  private final long totalUnderUtilizedMemBytes;

  private final double avgUnderUtilizedCpu;
  private final double avgOverUtilizedCpu;
  private final double avgUnderUtilizedMemBytes;

  private final double maxUnderUtilizedCpu;
  private final double maxOverUtilizedCpu;
  private final long maxUnderUtilizedMemBytes;

  private final double minUnderUtilizedCpu;
  private final double minOverUtilizedCpu;
  private final long minUnderUtilizedMemBytes;

  @JsonCreator
  public ClusterUtilization(@JsonProperty("requestUtilizations") List<RequestUtilization> requestUtilizations,
                            @JsonProperty("numRequestsWithUnderUtilizedCpu") int numRequestsWithUnderUtilizedCpu,
                            @JsonProperty("numRequestsWithOverUtilizedCpu") int numRequestsWithOverUtilizedCpu,
                            @JsonProperty("numRequestsWithUnderUtilizedMemBytes") int numRequestsWithUnderUtilizedMemBytes,
                            @JsonProperty("totalUnderUtilizedCpu") double totalUnderUtilizedCpu,
                            @JsonProperty("totalOverUtilizedCpu") double totalOverUtilizedCpu,
                            @JsonProperty("totalUnderUtilizedMemBytes") long totalUnderUtilizedMemBytes,
                            @JsonProperty("avgUnderUtilizedCpu") double avgUnderUtilizedCpu,
                            @JsonProperty("avgOverUtilizedCpu") double avgOverUtilizedCpu,
                            @JsonProperty("avgUnderUtilizedMemBytes") double avgUnderUtilizedMemBytes,
                            @JsonProperty("maxUnderUtilizedCpu") double maxUnderUtilizedCpu,
                            @JsonProperty("maxOverUtilizedCpu") double maxOverUtilizedCpu,
                            @JsonProperty("maxUnderUtilizedMemBytes") long maxUnderUtilizedMemBytes,
                            @JsonProperty("minUnderUtilizedCpu") double minUnderUtilizedCpu,
                            @JsonProperty("minOverUtilizedCpu") double minOverUtilizedCpu,
                            @JsonProperty("minUnderUtilizedMemBytes) ") long minUnderUtilizedMemBytes) {
    this.requestUtilizations = requestUtilizations;
    this.numRequestsWithUnderUtilizedCpu = numRequestsWithUnderUtilizedCpu;
    this.numRequestsWithOverUtilizedCpu = numRequestsWithOverUtilizedCpu;
    this.numRequestsWithUnderUtilizedMemBytes = numRequestsWithUnderUtilizedMemBytes;
    this.totalUnderUtilizedCpu = totalUnderUtilizedCpu;
    this.totalOverUtilizedCpu = totalOverUtilizedCpu;
    this.totalUnderUtilizedMemBytes = totalUnderUtilizedMemBytes;
    this.avgUnderUtilizedCpu = avgUnderUtilizedCpu;
    this.avgOverUtilizedCpu = avgOverUtilizedCpu;
    this.avgUnderUtilizedMemBytes = avgUnderUtilizedMemBytes;
    this.maxUnderUtilizedCpu = maxUnderUtilizedCpu;
    this.maxOverUtilizedCpu = maxOverUtilizedCpu;
    this.maxUnderUtilizedMemBytes = maxUnderUtilizedMemBytes;
    this.minUnderUtilizedCpu = minUnderUtilizedCpu;
    this.minOverUtilizedCpu = minOverUtilizedCpu;
    this.minUnderUtilizedMemBytes = minUnderUtilizedMemBytes;
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

  public double getTotalUnderUtilizedCpu() {
    return totalUnderUtilizedCpu;
  }

  public double getTotalOverUtilizedCpu() {
    return totalOverUtilizedCpu;
  }

  public long getTotalUnderUtilizedMemBytes() {
    return totalUnderUtilizedMemBytes;
  }

  public double getAvgUnderUtilizedCpu() {
    return avgUnderUtilizedCpu;
  }

  public double getAvgOverUtilizedCpu() {
    return avgOverUtilizedCpu;
  }

  public double getAvgUnderUtilizedMemBytes() {
    return avgUnderUtilizedMemBytes;
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

  public double getMinUnderUtilizedCpu() {
    return minUnderUtilizedCpu;
  }

  public double getMinOverUtilizedCpu() {
    return minOverUtilizedCpu;
  }

  public long getMinUnderUtilizedMemBytes() {
    return minUnderUtilizedMemBytes;
  }
}
