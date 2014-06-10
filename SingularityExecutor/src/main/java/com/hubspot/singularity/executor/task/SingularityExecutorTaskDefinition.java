package com.hubspot.singularity.executor.task;

import java.nio.file.Path;

import com.hubspot.deploy.ExecutorData;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;

public class SingularityExecutorTaskDefinition {

  private final ExecutorData executorData;
  private final String taskId;
  
  private final Path taskDirectory;
  private final Path executorBashOut;
  private final Path serviceLogOut;
  
  public SingularityExecutorTaskDefinition(String taskId, ExecutorData executorData, SingularityExecutorConfiguration configuration) {
    this.executorData = executorData;
    this.taskId = taskId;
  
    this.serviceLogOut = configuration.getTaskDirectoryPath(taskId).resolve(configuration.getServiceLog());
    this.taskDirectory = configuration.getTaskDirectoryPath(taskId);
    this.executorBashOut = configuration.getExecutorBashLogPath(taskId);
  }

  public ExecutorData getExecutorData() {
    return executorData;
  }

  public Path getTaskDirectory() {
    return taskDirectory;
  }

  public Path getExecutorBashOut() {
    return executorBashOut;
  }

  public Path getServiceLogOut() {
    return serviceLogOut;
  }

  public String getTaskId() {
    return taskId;
  }

  @Override
  public String toString() {
    return "SingularityExecutorTaskDefinition [taskId=" + taskId + "]";
  }
  
  
}
