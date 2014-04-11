package com.hubspot.singularity.executor.models;

import java.util.Map;

import com.hubspot.deploy.ExecutorData;

public class EnvironmentContext {
  
  private final ExecutorData executorData;
  private final Map<String, String> deployConfigEnv;
  
  public EnvironmentContext(ExecutorData executorData, Map<String, String> deployConfigEnv) {
    this.executorData = executorData;
    this.deployConfigEnv = deployConfigEnv;
  }

  public ExecutorData getExecutorData() {
    return executorData;
  }

  public Map<String, String> getDeployConfigEnv() {
    return deployConfigEnv;
  }

  @Override
  public String toString() {
    return "EnvironmentContext [executorData=" + executorData + ", deployConfigEnv=" + deployConfigEnv + "]";
  }
  
}
