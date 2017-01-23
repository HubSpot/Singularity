package com.hubspot.singularity.executor.models;

import java.util.ArrayList;
import java.util.List;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.Environment.Variable;
import org.apache.mesos.Protos.Parameter;
import org.apache.mesos.Protos.TaskInfo;

import com.google.common.base.Strings;

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

  public List<String> getDockerParameters() {
    List<String> args = new ArrayList<>();
    for (Parameter parameter : taskInfo.getContainer().getDocker().getParametersList()) {
      args.add(toCmdLineArg(parameter));
    }
    return args;
  }

  public boolean isDockerWorkdirOverriden() {
    for (Parameter parameter : taskInfo.getContainer().getDocker().getParametersList()) {
      if (parameter.hasKey() && (parameter.getKey().equals("w") || parameter.getKey().equals("workdir"))) {
        return true;
      }
    }
    return false;
  }

  public List<Protos.Volume> getContainerVolumes() {
    return taskInfo.getContainer().getVolumesList();
  }

  private String toCmdLineArg(Parameter parameter) {
    if (parameter.hasKey() && parameter.getKey().length() > 1) {
      if (parameter.hasValue() && !Strings.isNullOrEmpty(parameter.getValue())) {
        return String.format("--%s=%s", parameter.getKey(), parameter.getValue());
      } else {
        return String.format("--%s", parameter.getKey());
      }
    } else {
      if (parameter.hasValue() && !Strings.isNullOrEmpty(parameter.getValue())) {
        return String.format("-%s=%s", parameter.getKey(), parameter.getValue());
      } else {
        return String.format("-%s", parameter.getKey());
      }
    }
  }

  @Override
  public String toString() {
    return "EnvironmentContext [taskInfo=" + taskInfo + "]";
  }

}
