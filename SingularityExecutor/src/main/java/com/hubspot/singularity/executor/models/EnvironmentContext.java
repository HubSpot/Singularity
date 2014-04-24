package com.hubspot.singularity.executor.models;

import java.util.List;

import org.apache.mesos.Protos.Environment.Variable;
import org.apache.mesos.Protos.TaskInfo;

public class EnvironmentContext {
  
  private final TaskInfo taskInfo;
  
  public EnvironmentContext(TaskInfo taskInfo) {
    this.taskInfo = taskInfo;
  }

  public List<Variable> getEnv() {
    return taskInfo.getExecutor().getCommand().getEnvironment().getVariablesList();
  }

  @Override
  public String toString() {
    return "EnvironmentContext [taskInfo=" + taskInfo + "]";
  }
  
}
