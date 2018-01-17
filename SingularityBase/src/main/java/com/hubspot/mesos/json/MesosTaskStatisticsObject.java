package com.hubspot.mesos.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MesosTaskStatisticsObject {
  private final int cpusLimit;
  private final long cpusNrPeriods;
  private final long cpusNrThrottled;
  private final double cpusSystemTimeSecs;
  private final double cpusThrottledTimeSecs;
  private final double cpusUserTimeSecs;
  private final long memAnonBytes;
  private final long memFileBytes;
  private final long memLimitBytes;
  private final long memMappedFileBytes;
  private final long memRssBytes;
  private final long memTotalBytes;
  private final long diskLimitBytes;
  private final long diskUsedBytes;
  private final double timestampSeconds;

  @JsonCreator
  public MesosTaskStatisticsObject(@JsonProperty("cpus_limit") int cpusLimit,
                                   @JsonProperty("cpus_nr_periods") long cpusNrPeriods,
                                   @JsonProperty("cpus_nr_throttled") long cpusNrThrottled,
                                   @JsonProperty("cpus_system_time_secs") double cpusSystemTimeSecs,
                                   @JsonProperty("cpus_throttled_time_secs") double cpusThrottledTimeSecs,
                                   @JsonProperty("cpus_user_time_secs") double cpusUserTimeSecs,
                                   @JsonProperty("mem_anon_bytes") long memAnonBytes,
                                   @JsonProperty("mem_file_bytes") long memFileBytes,
                                   @JsonProperty("mem_limit_bytes") long memLimitBytes,
                                   @JsonProperty("mem_mapped_file_bytes") long memMappedFileBytes,
                                   @JsonProperty("mem_rss_bytes") long memRssBytes,
                                   @JsonProperty("mem_total_bytes") long memTotalBytes,
                                   @JsonProperty("disk_limit_bytes") long diskLimitBytes,
                                   @JsonProperty("disk_used_bytes") long diskUsedBytes,
                                   @JsonProperty("timestamp") double timestampSeconds) {
    this.cpusLimit = cpusLimit;
    this.cpusNrPeriods = cpusNrPeriods;
    this.cpusNrThrottled = cpusNrThrottled;
    this.cpusSystemTimeSecs = cpusSystemTimeSecs;
    this.cpusThrottledTimeSecs = cpusThrottledTimeSecs;
    this.cpusUserTimeSecs = cpusUserTimeSecs;
    this.memAnonBytes = memAnonBytes;
    this.memFileBytes = memFileBytes;
    this.memLimitBytes = memLimitBytes;
    this.memMappedFileBytes = memMappedFileBytes;
    this.memRssBytes = memRssBytes;
    this.memTotalBytes = memTotalBytes;
    this.diskLimitBytes = diskLimitBytes;
    this.diskUsedBytes = diskUsedBytes;
    this.timestampSeconds = timestampSeconds;
  }

  public int getCpusLimit() {
    return cpusLimit;
  }

  public long getCpusNrPeriods() {
    return cpusNrPeriods;
  }

  public long getCpusNrThrottled() {
    return cpusNrThrottled;
  }

  public double getCpusSystemTimeSecs() {
    return cpusSystemTimeSecs;
  }

  public double getCpusThrottledTimeSecs() {
    return cpusThrottledTimeSecs;
  }

  public double getCpusUserTimeSecs() {
    return cpusUserTimeSecs;
  }

  public long getMemAnonBytes() {
    return memAnonBytes;
  }

  public long getMemFileBytes() {
    return memFileBytes;
  }

  public long getMemLimitBytes() {
    return memLimitBytes;
  }

  public long getMemMappedFileBytes() {
    return memMappedFileBytes;
  }

  public long getMemRssBytes() {
    return memRssBytes;
  }

  public long getMemTotalBytes() {
    return memTotalBytes;
  }

  public long getDiskLimitBytes() {
    return diskLimitBytes;
  }

  public long getDiskUsedBytes() {
    return diskUsedBytes;
  }

  public double getTimestampSeconds() {
    return timestampSeconds;
  }

  @Override
  public String toString() {
    return "MesosTaskStatisticsObject{" +
        "cpusLimit=" + cpusLimit +
        ", cpusNrPeriods=" + cpusNrPeriods +
        ", cpusNrThrottled=" + cpusNrThrottled +
        ", cpusSystemTimeSecs=" + cpusSystemTimeSecs +
        ", cpusThrottledTimeSecs=" + cpusThrottledTimeSecs +
        ", cpusUserTimeSecs=" + cpusUserTimeSecs +
        ", memAnonBytes=" + memAnonBytes +
        ", memFileBytes=" + memFileBytes +
        ", memLimitBytes=" + memLimitBytes +
        ", memMappedFileBytes=" + memMappedFileBytes +
        ", memRssBytes=" + memRssBytes +
        ", memTotalBytes=" + memTotalBytes +
        ", timestampSeconds=" + timestampSeconds +
        '}';
  }
}
