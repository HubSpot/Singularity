package com.hubspot.singularity.executor.models;

import com.google.common.collect.Maps;
import com.hubspot.deploy.ExecutorData;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class EnvironmentContext {
  
  private final ExecutorData executorData;
  private final List<Long> ports;
  private final Map<String, Long> portsEnv;
  
  public EnvironmentContext(ExecutorData executorData, List<Long> ports) {
    this.executorData = executorData;
    this.ports = ports;

    portsEnv = Maps.newHashMap();

    if (ports != null && !ports.isEmpty()) {
      for (int i=0; i<ports.size(); i++) {
        portsEnv.put("", ports.get(0));
        portsEnv.put(Integer.toString(i), ports.get(i));
      }
    }
  }

  public Set<Map.Entry<String, String>> getExecutorDataEnv() {
    return executorData.getEnv().entrySet();
  }

  public Set<Map.Entry<String, Long>> getPortsEnv() {
    return portsEnv.entrySet();
  }

  @Override
  public String toString() {
    return "EnvironmentContext [" +
        "executorData=" + executorData +
        ", ports=" + ports +
        ']';
  }
}
