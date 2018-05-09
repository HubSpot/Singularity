package com.hubspot.singularity.metrics;

import java.util.Map;

import com.codahale.metrics.Metric;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(title = "A container for metrics")
public class SingularityMetricsContainer {
  private final Map<String, Metric> metrics;

  @JsonCreator
  public SingularityMetricsContainer(@JsonProperty("metrics") Map<String, Metric> metrics) {
    this.metrics = metrics;
  }

  @Schema(title = "A map of metric name to metric content")
  public Map<String, Metric> getMetrics() {
    return metrics;
  }

  @Override
  public String toString() {
    return "SingularityMetricsContainer{" +
        "metrics=" + metrics +
        '}';
  }
}
