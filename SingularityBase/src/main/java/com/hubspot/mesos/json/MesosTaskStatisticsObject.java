package com.hubspot.mesos.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Describes the current resource usage for a task")
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
  private final double timestamp;

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
                                   @JsonProperty("timestamp") double timestamp) {
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
    this.timestamp = timestamp;
  }

  @Schema(description = "The cpu limit for this task")
  public int getCpusLimit() {
    return cpusLimit;
  }

  @Schema(description = "From cgroups cpu.stat")
  public long getCpusNrPeriods() {
    return cpusNrPeriods;
  }

  @Schema(description = "From cgroups cpu.stat")
  public long getCpusNrThrottled() {
    return cpusNrThrottled;
  }

  @Schema(description = "From cgroups cpu.stat")
  public double getCpusSystemTimeSecs() {
    return cpusSystemTimeSecs;
  }

  @Schema(description = "From cgroups cpu.stat")
  public double getCpusThrottledTimeSecs() {
    return cpusThrottledTimeSecs;
  }

  @Schema(description = "The cpu seconds consumed by this task")
  public double getCpusUserTimeSecs() {
    return cpusUserTimeSecs;
  }

  @Schema(description = "Bytes of anonymous memory")
  public long getMemAnonBytes() {
    return memAnonBytes;
  }

  @Schema(description = "File memory used in bytes")
  public long getMemFileBytes() {
    return memFileBytes;
  }

  @Schema(description = "Memory limit of this task in bytes")
  public long getMemLimitBytes() {
    return memLimitBytes;
  }

  @Schema(description = "Mapped file memory used in bytes")
  public long getMemMappedFileBytes() {
    return memMappedFileBytes;
  }

  @Schema(description = "rss used in bytes")
  public long getMemRssBytes() {
    return memRssBytes;
  }

  @Schema(description = "Total memory used in bytes")
  public long getMemTotalBytes() {
    return memTotalBytes;
  }

  @Schema(description = "Disk space limit for this task in bytes")
  public long getDiskLimitBytes() {
    return diskLimitBytes;
  }

  @Schema(description = "Disk space used by this task in bytes")
  public long getDiskUsedBytes() {
    return diskUsedBytes;
  }

  @Schema(description = "Timestamp in seconds at which this usage was collected")
  public double getTimestamp() {
    return timestamp;
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
        ", timestamp=" + timestamp +
        '}';
  }
}
