package com.hubspot.singularity.metrics;

import java.util.Map;

import com.codahale.metrics.Metric;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityMetricsContainer {
  private final Map<String, Metric> metrics;

  @JsonCreator
  public SingularityMetricsContainer(@JsonProperty("metrics") Map<String, Metric> metrics) {
    this.metrics = metrics;
  }

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
