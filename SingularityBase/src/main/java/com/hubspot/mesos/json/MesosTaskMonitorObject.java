package com.hubspot.mesos.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MesosTaskMonitorObject {
  private final String executorId;
  private final String executorName;
  private final String frameworkId;
  private final String source;
  private final MesosTaskStatisticsObject statistics;

  @JsonCreator
  public MesosTaskMonitorObject(@JsonProperty("executor_id") String executorId,
      @JsonProperty("executor_name") String executorName,
      @JsonProperty("framework_id") String frameworkId,
      @JsonProperty("source") String source,
      @JsonProperty("statistics") MesosTaskStatisticsObject statistics) {
    this.executorId = executorId;
    this.executorName = executorName;
    this.frameworkId = frameworkId;
    this.source = source;
    this.statistics = statistics;
  }

  public String getExecutorId() {
    return executorId;
  }

  public String getExecutorName() {
    return executorName;
  }

  public String getFrameworkId() {
    return frameworkId;
  }

  public String getSource() {
    return source;
  }

  public MesosTaskStatisticsObject getStatistics() {
    return statistics;
  }

  @Override
  public String toString() {
    return "MesosTaskMonitorObject [executorId=" + executorId + ", executorName=" + executorName + ", frameworkId=" + frameworkId + ", source=" + source + ", statistics=" + statistics + "]";
  }

}
