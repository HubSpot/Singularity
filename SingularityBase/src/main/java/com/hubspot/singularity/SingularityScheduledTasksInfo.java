package com.hubspot.singularity;

import java.util.List;

public class SingularityScheduledTasksInfo {
  
  private final int numLateTasks;
  private final int numFutureTasks;
  private final long maxTaskLag;
  private final long timestamp;

  private SingularityScheduledTasksInfo(int numLateTasks, int numFutureTasks, long maxTaskLag, long timestamp) {
    this.numLateTasks = numLateTasks;
    this.numFutureTasks = numFutureTasks;
    this.maxTaskLag = maxTaskLag;
    this.timestamp = timestamp;
  }

  public int getNumLateTasks() {
    return numLateTasks;
  }

  public int getNumFutureTasks() {
    return numFutureTasks;
  }

  public long getMaxTaskLag() {
    return maxTaskLag;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public static SingularityScheduledTasksInfo getInfo(List<SingularityPendingTaskId> pendingTaskIds, long millisDeltaForLateTasks) {
    final long now = System.currentTimeMillis();
    
    int numLateTasks = 0;
    int numFutureTasks = 0;
    long maxTaskLag = 0;
    
    for (SingularityPendingTaskId pendingTaskId : pendingTaskIds) {
      long delta = now - pendingTaskId.getNextRunAt();
      
      if (delta > millisDeltaForLateTasks) {
        numLateTasks++;
      } else {
        numFutureTasks++;
      }

      if (delta > maxTaskLag) {
        maxTaskLag = delta;
      }
    }

    return new SingularityScheduledTasksInfo(numLateTasks, numFutureTasks, maxTaskLag, now);
  }
}
