package com.hubspot.singularity.executor.task;

import java.nio.file.Path;

import com.hubspot.deploy.ExecutorData;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;

public class SingularityExecutorTaskConfiguration {

  private final SingularityExecutorTaskDefinition taskDefinition;
  
  private final Path taskDirectory;
  private final Path executorBashOut;
  private final Path serviceLogOut;
  
  public SingularityExecutorTaskConfiguration(SingularityExecutorTaskDefinition taskDefinition, SingularityExecutorConfiguration configuration) {
    this.taskDefinition = taskDefinition;
    
    this.serviceLogOut = configuration.getTaskDirectoryPath(taskDefinition.getTaskId()).resolve(configuration.getServiceLog());
    this.taskDirectory = configuration.getTaskDirectoryPath(taskDefinition.getTaskId());
    this.executorBashOut = configuration.getExecutorBashLogPath(taskDefinition.getTaskId());
  }
    
  public ExecutorData getExecutorData() {
    return taskDefinition.getExecutorData();
  }

  public String getTaskId() {
    return taskDefinition.getTaskId();
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
  
  

}
