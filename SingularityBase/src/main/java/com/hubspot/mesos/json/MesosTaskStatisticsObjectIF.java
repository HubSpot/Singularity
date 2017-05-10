package com.hubspot.mesos.json;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
public interface MesosTaskStatisticsObjectIF {

  @JsonProperty("cpus_limit")
  int getCpusLimit();

  @JsonProperty("cpus_nr_periods")
  long getCpusNrPeriods();

  @JsonProperty("cpus_nr_throttled")
  long getCpusNrThrottled();

  @JsonProperty("cpus_system_time_secs")
  double getCpusSystemTimeSecs();

  @JsonProperty("cpus_throttled_time_secs")
  double getCpusThrottledTimeSecs();

  @JsonProperty("cpus_user_time_secs")
  double getCpusUserTimeSecs();

  @JsonProperty("mem_anon_bytes")
  long getMemAnonBytes();

  @JsonProperty("mem_file_bytes")
  long getMemFileBytes();

  @JsonProperty("mem_limit_bytes")
  long getMemLimitBytes();

  @JsonProperty("mem_mapped_file_bytes")
  long getMemMappedFileBytes();

  @JsonProperty("mem_rss_bytes")
  long getMemRssBytes();

  @JsonProperty("timestamp")
  double getTimestamp();

}
