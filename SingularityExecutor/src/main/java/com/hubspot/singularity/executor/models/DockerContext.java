package com.hubspot.singularity.executor.models;

import java.util.List;

public class DockerContext {
  private final EnvironmentContext envContext;
  private final RunnerContext runContext;
  private final String prefix;
  private final int stopTimeout;
  private final boolean privileged;
  private final List<String> inheritVariables;

  public DockerContext(EnvironmentContext envContext, RunnerContext runContext, String prefix, int stopTimeout, boolean privileged, List<String> inheritVariables) {
    this.envContext = envContext;
    this.runContext = runContext;
    this.prefix = prefix;
    this.stopTimeout = stopTimeout;
    this.privileged = privileged;
    this.inheritVariables = inheritVariables;
  }

  public EnvironmentContext getEnvContext() {
    return envContext;
  }

  public RunnerContext getRunContext() {
    return runContext;
  }

  public String getPrefix() {
    return prefix;
  }

  public int getStopTimeout() {
    return stopTimeout;
  }

  public boolean isPrivileged() {
    return privileged;
  }

  public List<String> getInheritVariables() {
    return inheritVariables;
  }

  @Override
  public String toString() {
    return "DockerContext{" +
        "envContext=" + envContext +
        ", runContext=" + runContext +
        ", prefix='" + prefix + '\'' +
        ", stopTimeout=" + stopTimeout +
        ", privileged=" + privileged +
        ", inheritVariables=" + inheritVariables +
        '}';
  }
}
