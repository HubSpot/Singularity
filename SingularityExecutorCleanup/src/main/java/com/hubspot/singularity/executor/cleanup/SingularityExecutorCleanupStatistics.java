package com.hubspot.singularity.executor.cleanup;

public class SingularityExecutorCleanupStatistics {

  private final int totalTasks;
  private final int ioErrorTasks;
  private final int runningTasksIgnored;
  private final int runningTasks;
  private final int staleTasks;
  private final int successfullyCleanedTasks;
  private final int errorTasks;
  private final int invalidTasks; 
  
  public SingularityExecutorCleanupStatistics(int totalTasks, int runningTasks, int runningTasksIgnored, int staleTasks, int successfullyCleanedTasks, int ioErrorTasks, int errorTasks, int invalidTasks) {
    this.totalTasks = totalTasks;
    this.runningTasks = runningTasks;
    this.runningTasksIgnored = runningTasksIgnored;
    this.staleTasks = staleTasks;
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

  public int getTotalTasks() {
    return totalTasks;
  }

  public int getRunningTasks() {
    return runningTasks;
  }

  public int getStaleTasks() {
    return staleTasks;
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
    return "SingularityExecutorCleanupStatistics [totalTasks=" + totalTasks + ", ioErrorTasks=" + ioErrorTasks + ", runningTasksIgnored=" + runningTasksIgnored + ", runningTasks=" + runningTasks + ", staleTasks=" + staleTasks
        + ", successfullyCleanedTasks=" + successfullyCleanedTasks + ", errorTasks=" + errorTasks + ", invalidTasks=" + invalidTasks + "]";
  }

  public static class SingularityExecutorCleanupStatisticsBuilder {
    
    private int totalTasks;
    private int runningTasksIgnored;
    private int runningTasks;
    private int staleTasks;
    private int successfullyCleanedTasks;
    private int ioErrorTasks;
    private int errorTasks;
    private int invalidTasks; 
    
    public void incrTotalTasks() {
      totalTasks++;
    }
    
    public void incrRunningTasksIgnored() {
      runningTasksIgnored++;
    }
    
    public void incrIoErrorTasks() {
      ioErrorTasks++;
    }
    
    public void setRunningTasks(int runningTasks) {
      this.runningTasks = runningTasks;
    }
    
    public void incrStaleTasks() {
      staleTasks++;
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
      return new SingularityExecutorCleanupStatistics(totalTasks, runningTasks, runningTasksIgnored, staleTasks, successfullyCleanedTasks, ioErrorTasks, errorTasks, invalidTasks);
    }
    
  }
  
}
