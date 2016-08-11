package com.hubspot.singularity.executor.models;

import java.util.ArrayList;
import java.util.List;

import org.apache.mesos.Protos;
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

  public Protos.ContainerInfo.DockerInfo getDockerInfo() {
    return taskInfo.getContainer().getDocker();
  }

  public List<Protos.Parameter> getDockerParameters() {
    return taskInfo.getContainer().getDocker().getParametersList();
  }

  public List<Protos.Volume> getContainerVolumes() {
    return taskInfo.getContainer().getVolumesList();
  }

  public boolean isDocker() {
    return taskInfo.hasContainer() && taskInfo.getContainer().hasDocker();
  }

  @Override
  public String toString() {
    return "EnvironmentContext [taskInfo=" + taskInfo + "]";
  }

}
