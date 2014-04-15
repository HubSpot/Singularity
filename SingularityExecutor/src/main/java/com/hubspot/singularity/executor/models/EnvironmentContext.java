package com.hubspot.singularity.executor.models;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Objects;
import com.hubspot.deploy.ExecutorData;

public class EnvironmentContext {
  
  private final ExecutorData executorData;
  private final Map<String, String> deployConfigEnv;
  
  public EnvironmentContext(ExecutorData executorData, Map<String, String> deployConfigEnv) {
    this.executorData = executorData;
    this.deployConfigEnv = Objects.firstNonNull(deployConfigEnv, Collections.<String, String> emptyMap());
  }

  public Set<Map.Entry<String, String>> getExecutorDataEnv() {
    return executorData.getEnv().entrySet();
  }

  public Set<Map.Entry<String, String>> getDeployConfigEnv() {
    return deployConfigEnv.entrySet();
  }
  
  @Override
  public String toString() {
    return "EnvironmentContext [executorData=" + executorData + ", deployConfigEnv=" + deployConfigEnv + "]";
  }
  
}
