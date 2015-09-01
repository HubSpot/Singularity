package com.hubspot.singularity.executor.models;

public class DockerContext {
  private final EnvironmentContext envContext;
  private final RunnerContext runContext;
  private final String prefix;
  private final int stopTimeout;

  public DockerContext(EnvironmentContext envContext, RunnerContext runContext, String prefix, int stopTimeout) {
    this.envContext = envContext;
    this.runContext = runContext;
    this.prefix = prefix;
    this.stopTimeout = stopTimeout;
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

  @Override
  public String toString() {
    return "DockerContext [" +
      "envContext=" + envContext +
      "runContext=" + runContext +
      "prefix=" + prefix +
      "stopTimeout=" + stopTimeout +
      "]";

  }
}
