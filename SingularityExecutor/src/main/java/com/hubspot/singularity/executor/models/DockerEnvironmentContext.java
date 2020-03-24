package com.hubspot.singularity.executor.models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.mesos.Protos.Environment.Variable;
import org.apache.mesos.Protos.TaskInfo;

public class DockerEnvironmentContext extends EnvironmentContext {

  private final List<String> inheritVariables;

  public DockerEnvironmentContext(TaskInfo taskInfo, List<String> inheritVariables) {
    super(taskInfo);
    this.inheritVariables = inheritVariables;
  }

  @Override
  public List<Variable> getEnv() {
    List<Variable> variables = new ArrayList<>();
    Set<String> keys = new HashSet<>();

    inheritVariables.forEach((v) -> {
      if (!keys.contains(v)) {
        String val = System.getenv(v);
        if (val != null) {
          variables.add(Variable.newBuilder().setName(v).setValue(val).build());
          keys.add(v);
        }
      }
    });

    taskInfo.getExecutor()
        .getCommand()
        .getEnvironment()
        .getVariablesList()
        .forEach((v) -> {
          if (!keys.contains(v.getName())) {
            variables.add(v);
            keys.add(v.getName());
          }
        });
    return variables;
  }
}
