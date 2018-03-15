package com.hubspot.mesos.json;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "Describes the current resource usage for a task")
public interface MesosTaskStatisticsObjectIF {
  @JsonProperty("cpus_limit")
  @Schema(description = "The cpu limit for this task")
  int getCpusLimit();

  @JsonProperty("cpus_nr_periods")
  @Schema(description = "From cgroups cpu.stat")
  long getCpusNrPeriods();

  @JsonProperty("cpus_nr_throttled")
  @Schema(description = "From cgroups cpu.stat")
  long getCpusNrThrottled();

  @JsonProperty("cpus_system_time_secs")
  @Schema(description = "From cgroups cpu.stat")
  double getCpusSystemTimeSecs();

  @JsonProperty("cpus_throttled_time_secs")
  @Schema(description = "From cgroups cpu.stat")
  double getCpusThrottledTimeSecs();

  @JsonProperty("cpus_user_time_secs")
  @Schema(description = "The cpu seconds consumed by this task")
  double getCpusUserTimeSecs();

  @JsonProperty("mem_anon_bytes")
  @Schema(description = "Bytes of anonymous memory")
  long getMemAnonBytes();

  @JsonProperty("mem_file_bytes")
  @Schema(description = "File memory used in bytes")
  long getMemFileBytes();

  @JsonProperty("mem_limit_bytes")
  @Schema(description = "Memory limit of this task in bytes")
  long getMemLimitBytes();

  @JsonProperty("mem_mapped_file_bytes")
  @Schema(description = "Mapped file memory used in bytes")
  long getMemMappedFileBytes();

  @JsonProperty("mem_rss_bytes")
  @Schema(description = "rss used in bytes")
  long getMemRssBytes();

  @JsonProperty("mem_total_bytes")
  @Schema(description = "Total memory used in bytes")
  long getMemTotalBytes();

  @JsonProperty("disk_limit_bytes")
  @Schema(description = "Disk space limit for this task in bytes")
  long getDiskLimitBytes();

  @JsonProperty("disk_used_bytes")
  @Schema(description = "Disk space used by this task in bytes")
  long getDiskUsedBytes();

  @JsonProperty("timestamp")
  @Schema(description = "Timestamp in seconds at which this usage was collected")
  double getTimestampSeconds();
}
