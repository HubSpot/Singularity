package com.hubspot.singularity.executor.models;

public class DockerContext {
  private final EnvironmentContext envContext;
  private final RunnerContext runContext;

  public DockerContext(EnvironmentContext envContext, RunnerContext runContext) {
    this.envContext = envContext;
    this.runContext = runContext;
  }

  public EnvironmentContext getEnvContext() {
    return envContext;
  }

  public RunnerContext getRunContext() {
    return runContext;
  }

  @Override
  public String toString() {
    return "DockerContext [" +
      "envContext=" + envContext +
      "runContext=" + runContext +
      "]";

  }
}
