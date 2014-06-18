package com.hubspot.singularity.executor.task;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.deploy.ExecutorData;

public class SingularityExecutorTaskDefinition {

  private final ExecutorData executorData;
  private final String taskId;
  private final Path taskDirectoryPath;
  private final String executorBashOut;
  private final String serviceLogOut;
  private final String taskAppDirectory;
  private final String logrotateStateFile;
  
  @JsonCreator
  public SingularityExecutorTaskDefinition(@JsonProperty("taskId") String taskId, @JsonProperty("executorData") ExecutorData executorData, @JsonProperty("taskDirectory") String taskDirectory, 
      @JsonProperty("serviceLogOut") String serviceLogOut, @JsonProperty("taskAppDirectory") String taskAppDirectory, @JsonProperty("executorBashOut") String executorBashOut, @JsonProperty("logrotateStateFilePath") String logrotateStateFile) {
    this.executorData = executorData;
    this.taskId = taskId;
    this.taskDirectoryPath = Paths.get(taskDirectory);

    this.executorBashOut = executorBashOut;
    this.serviceLogOut = serviceLogOut;
    this.taskAppDirectory = taskAppDirectory;
    this.logrotateStateFile = logrotateStateFile;
  }

  @JsonIgnore
  public Path getTaskDirectoryPath() {
    return taskDirectoryPath;
  }

  @JsonIgnore
  public Path getExecutorBashOutPath() {
    return taskDirectoryPath.resolve(executorBashOut);
  }

  @JsonIgnore
  public Path getServiceLogOutPath() {
    return taskDirectoryPath.resolve(serviceLogOut);
  }
  
  @JsonIgnore
  public Path getTaskAppDirectoryPath() {
    return taskDirectoryPath.resolve(taskAppDirectory);
  }
  
  @JsonIgnore
  public Path getLogrotateStateFilePath() {
    return taskDirectoryPath.resolve(logrotateStateFile);
  }
  
  public String getExecutorBashOut() {
    return executorBashOut;
  }

  public String getServiceLogOut() {
    return serviceLogOut;
  }

  public String getTaskAppDirectory() {
    return taskAppDirectory;
  }

  public String getLogrotateStateFile() {
    return logrotateStateFile;
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
