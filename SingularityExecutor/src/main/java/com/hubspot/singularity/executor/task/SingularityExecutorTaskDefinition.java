package com.hubspot.singularity.executor.task;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;

public class SingularityExecutorTaskDefinition {

  private final ExecutorData executorData;
  private final String taskId;
  private final String taskDirectory;
  private final String executorBashOut;
  private final String serviceLogOut;
  
  public SingularityExecutorTaskDefinition(String taskId, ExecutorData executorData, SingularityExecutorConfiguration configuration) {
    this.taskId = taskId;
    this.executorData = executorData;
    this.serviceLogOut = configuration.getTaskDirectoryPath(taskId).resolve(configuration.getServiceLog()).toString();
    this.taskDirectory = configuration.getTaskDirectoryPath(taskId).toString();
    this.executorBashOut = configuration.getExecutorBashLogPath(taskId).toString();
  }
  
  @JsonCreator
  public SingularityExecutorTaskDefinition(@JsonProperty("taskId") String taskId, @JsonProperty("executorData") ExecutorData executorData, @JsonProperty("serviceLogOut") String serviceLogOut, 
      @JsonProperty("taskDirectory") String taskDirectory, @JsonProperty("executorBashOut") String executorBashOut) {
    this.executorData = executorData;
    this.taskId = taskId;
    this.taskDirectory = taskDirectory;
    this.executorBashOut = executorBashOut;
    this.serviceLogOut = serviceLogOut;
  }

  @JsonIgnore
  public Path getTaskDirectoryPath() {
    return Paths.get(taskDirectory);
  }

  @JsonIgnore
  public Path getExecutorBashOutPath() {
    return Paths.get(executorBashOut);
  }

  @JsonIgnore
  public Path getServiceLogOutPath() {
    return Paths.get(serviceLogOut);
  }
  
  public String getTaskDirectory() {
    return taskDirectory;
  }

  public String getExecutorBashOut() {
    return executorBashOut;
  }

  public String getServiceLogOut() {
    return serviceLogOut;
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
