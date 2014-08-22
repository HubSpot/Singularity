package com.hubspot.singularity.executor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class SingularityExecutorCleanupStatistics {

  private final int totalTaskFiles;
  private final int ioErrorTasks;
  private final int runningTasksIgnored;
  private final int mesosRunningTasks;
  private final int successfullyCleanedTasks;
  private final int errorTasks;
  private final int invalidTasks;

  @JsonCreator
  public SingularityExecutorCleanupStatistics(
      @JsonProperty("totalTaskFiles") int totalTaskFiles,
      @JsonProperty("mesosRunningTasks") int mesosRunningTasks,
      @JsonProperty("runningTasksIgnored") int runningTasksIgnored,
      @JsonProperty("successfullyCleanedTasks") int successfullyCleanedTasks,
      @JsonProperty("ioErrorTasks") int ioErrorTasks,
      @JsonProperty("errorTasks") int errorTasks,
      @JsonProperty("invalidTasks") int invalidTasks
    ) {
    this.totalTaskFiles = totalTaskFiles;
    this.mesosRunningTasks = mesosRunningTasks;
    this.runningTasksIgnored = runningTasksIgnored;
    this.ioErrorTasks = ioErrorTasks;
    this.successfullyCleanedTasks = successfullyCleanedTasks;
    this.errorTasks = errorTasks;
    this.invalidTasks = invalidTasks;
  }

  public int getRunningTasksIgnored() {
    return runningTasksIgnored;
  }

  public int getIoErrorTasks() {
    return ioErrorTasks;
  }

  public int getTotalTaskFiles() {
    return totalTaskFiles;
  }

  public int getMesosRunningTasks() {
    return mesosRunningTasks;
  }


  public int getSuccessfullyCleanedTasks() {
    return successfullyCleanedTasks;
  }

  public int getErrorTasks() {
    return errorTasks;
  }

  public int getInvalidTasks() {
    return invalidTasks;
  }

  @Override
  public String toString() {
    return "SingularityExecutorCleanupStatistics [totalTaskFiles=" + totalTaskFiles + ", ioErrorTasks=" + ioErrorTasks + ", runningTasksIgnored=" + runningTasksIgnored + ", mesosRunningTasks=" + mesosRunningTasks
        + ", successfullyCleanedTasks=" + successfullyCleanedTasks + ", errorTasks=" + errorTasks + ", invalidTasks=" + invalidTasks + "]";
  }

  public static class SingularityExecutorCleanupStatisticsBuilder {

    private int totalTaskFiles;
    private int runningTasksIgnored;
    private int mesosRunningTasks;
    private int successfullyCleanedTasks;
    private int ioErrorTasks;
    private int errorTasks;
    private int invalidTasks;

    public void incrTotalTaskFiles() {
      totalTaskFiles++;
    }

    public void incrRunningTasksIgnored() {
      runningTasksIgnored++;
    }

    public void incrIoErrorTasks() {
      ioErrorTasks++;
    }

    public void setMesosRunningTasks(int mesosRunningTasks) {
      this.mesosRunningTasks = mesosRunningTasks;
    }

    public void incrErrorTasks() {
      errorTasks++;
    }

    public void incrSuccessfullyCleanedTasks() {
      successfullyCleanedTasks++;
    }

    public void incrInvalidTasks() {
      invalidTasks++;
    }

    public SingularityExecutorCleanupStatistics build() {
      return new SingularityExecutorCleanupStatistics(totalTaskFiles, mesosRunningTasks, runningTasksIgnored, successfullyCleanedTasks, ioErrorTasks, errorTasks, invalidTasks);
    }

  }

}
