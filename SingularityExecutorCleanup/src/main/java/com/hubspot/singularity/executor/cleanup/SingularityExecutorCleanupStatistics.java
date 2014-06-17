package com.hubspot.singularity.executor.cleanup;

public class SingularityExecutorCleanupStatistics {

  private final int totalTasks;
  private final int ioErrorTasks;
  private final int runningTasks;
  private final int staleTasks;
  private final int successfullyCleanedTasks;
  private final int errorTasks;
  private final int invalidTasks; 
  
  public SingularityExecutorCleanupStatistics(int totalTasks, int runningTasks, int staleTasks, int successfullyCleanedTasks, int ioErrorTasks, int errorTasks, int invalidTasks) {
    this.totalTasks = totalTasks;
    this.runningTasks = runningTasks;
    this.staleTasks = staleTasks;
    this.ioErrorTasks = ioErrorTasks;
    this.successfullyCleanedTasks = successfullyCleanedTasks;
    this.errorTasks = errorTasks;
    this.invalidTasks = invalidTasks;
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

  public static class SingularityExecutorCleanupStatisticsBuilder {
    
    private int totalTasks;
    private int runningTasks;
    private int staleTasks;
    private int successfullyCleanedTasks;
    private int ioErrorTasks;
    private int errorTasks;
    private int invalidTasks; 
    
    public void incrTotalTasks() {
      totalTasks++;
    }
    
    public void incrIoErrorTasks() {
      ioErrorTasks++;
    }
    
    public void incrRunningTasks() {
      runningTasks++;
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
      return new SingularityExecutorCleanupStatistics(totalTasks, runningTasks, staleTasks, successfullyCleanedTasks, ioErrorTasks, errorTasks, invalidTasks);
    }
    
  }
  
}
