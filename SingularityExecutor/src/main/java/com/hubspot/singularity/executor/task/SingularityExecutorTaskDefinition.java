package com.hubspot.singularity.executor.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.deploy.ExecutorData;

public class SingularityExecutorTaskDefinition {

  private final ExecutorData executorData;
  private final String taskId;
  
  @JsonCreator
  public SingularityExecutorTaskDefinition(@JsonProperty("taskId") String taskId, @JsonProperty("executorData") ExecutorData executorData) {
    this.executorData = executorData;
    this.taskId = taskId;
  }
 
  public ExecutorData getExecutorData() {
    return executorData;
  }

  public String getTaskId() {
    return taskId;
  }

  @Override
  public String toString() {
    return "SingularityExecutorTaskDefinition [taskId=" + taskId + "]";
  }
  
  
}
