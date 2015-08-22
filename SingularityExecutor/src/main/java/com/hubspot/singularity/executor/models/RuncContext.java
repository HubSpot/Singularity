package com.hubspot.singularity.executor.models;

import java.util.Arrays;
import java.util.List;

import com.hubspot.singularity.SingularityRuncConfig;

public class RuncContext {
  private final EnvironmentContext envContext;
  private final RunnerContext runContext;
  private final SingularityRuncConfig runcConfig;
  private final int gid;
  private final int uid;
  private final String os;
  private final String arch;
  private final List<String> commandArgs;

  public RuncContext(EnvironmentContext envContext, RunnerContext runContext, SingularityRuncConfig runcConfig) {
    this.envContext = envContext;
    this.runContext = runContext;
    this.runcConfig = runcConfig;
    this.uid = findUid();
    this.gid = findGid();
    this.os = findOs();
    this.arch = findArch();
    this.commandArgs = Arrays.asList(runContext.getCmd().split(" "));
  }

  private int findUid() {
    return 0;
  }

  private int findGid() {
    return 0;
  }

  private String findOs() {
    return "";
  }

  private String findArch() {
    return "";
  }

  public EnvironmentContext getEnvContext() {
    return envContext;
  }

  public RunnerContext getRunContext() {
    return runContext;
  }

  public SingularityRuncConfig getRuncConfig() {
    return runcConfig;
  }

  public int getGid() {
    return gid;
  }

  public int getUid() {
    return uid;
  }

  public String getOs() {
    return os;
  }

  public String getArch() {
    return arch;
  }

  public List<String> getCommandArgs() {
    return commandArgs;
  }

  @Override
  public String toString() {
    return "DockerContext [" +
      "envContext=" + envContext +
      "runContext=" + runContext +
      "runcConfig=" + runcConfig +
      "]";

  }

}
