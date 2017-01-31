package com.hubspot.singularity.config;

import javax.validation.constraints.Min;

import org.hibernate.validator.constraints.NotEmpty;

public class CustomExecutorConfiguration {
  @Min(0)
  private double numCpus = 0;

  @Min(0)
  private int memoryMb = 0;

  @Min(0)
  private int diskMb = 0;

  @NotEmpty
  private String serviceLog = "service.log";

  @NotEmpty
  private String serviceFinishedTailLog = "tail_of_finished_service.log";

  public double getNumCpus() {
    return numCpus;
  }

  public void setNumCpus(double numCpus) {
    this.numCpus = numCpus;
  }

  public int getMemoryMb() {
    return memoryMb;
  }

  public void setMemoryMb(int memoryMb) {
    this.memoryMb = memoryMb;
  }

  public int getDiskMb() {
    return diskMb;
  }

  public void setDiskMb(int diskMb) {
    this.diskMb = diskMb;
  }

  public String getServiceLog() {
    return serviceLog;
  }

  public void setServiceLog(String serviceLog) {
    this.serviceLog = serviceLog;
  }

  public String getServiceFinishedTailLog() {
    return serviceFinishedTailLog;
  }

  public void setServiceFinishedTailLog(String serviceFinishedTailLog) {
    this.serviceFinishedTailLog = serviceFinishedTailLog;
  }
}
