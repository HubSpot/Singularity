package com.hubspot.singularity.config;

import javax.validation.constraints.NotNull;

public class MesosConfiguration {

  @NotNull
  private String master;
  @NotNull
  private String frameworkName;
  @NotNull
  private Double frameworkFailoverTimeout;
  @NotNull
  private Integer defaultCpus;
  @NotNull
  private Integer defaultMemory;

  public String getMaster() {
    return master;
  }

  public String getFrameworkName() {
    return frameworkName;
  }

  public void setFrameworkName(String frameworkName) {
    this.frameworkName = frameworkName;
  }

  public Double getFrameworkFailoverTimeout() {
    return frameworkFailoverTimeout;
  }

  public void setFrameworkFailoverTimeout(Double frameworkFailoverTimeout) {
    this.frameworkFailoverTimeout = frameworkFailoverTimeout;
  }

  public void setMaster(String master) {
    this.master = master;
  }

  public Integer getDefaultCpus() {
    return defaultCpus;
  }

  public void setDefaultCpus(Integer defaultCpus) {
    this.defaultCpus = defaultCpus;
  }

  public Integer getDefaultMemory() {
    return defaultMemory;
  }

  public void setDefaultMemory(Integer defaultMemory) {
    this.defaultMemory = defaultMemory;
  }

}
