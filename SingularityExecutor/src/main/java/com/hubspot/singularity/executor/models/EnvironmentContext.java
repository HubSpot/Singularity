package com.hubspot.singularity.executor.models;

import java.util.Map;
import java.util.Set;

import com.hubspot.deploy.ExecutorData;

public class EnvironmentContext {
  
  private final ExecutorData executorData;
  
  public EnvironmentContext(ExecutorData executorData) {
    this.executorData = executorData;
  }

  public Set<Map.Entry<String, String>> getExecutorDataEnv() {
    return executorData.getEnv().entrySet();
  }

  @Override
  public String toString() {
    return "EnvironmentContext [executorData=" + executorData + "]";
  }
  
}
