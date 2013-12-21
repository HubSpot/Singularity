package com.hubspot.singularity;

import java.util.List;

public class SingularityScheduledTasksInfo {
  private final int numLateTasks;
  private final int numFutureTasks;
  private final long maxTaskLag;

  private SingularityScheduledTasksInfo(int numLateTasks, int numFutureTasks, long maxTaskLag) {
    this.numLateTasks = numLateTasks;
    this.numFutureTasks = numFutureTasks;
    this.maxTaskLag = maxTaskLag;
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

  public static SingularityScheduledTasksInfo getInfo(List<SingularityPendingTaskId> pendingTaskIds) {
    final long now = System.currentTimeMillis();

    int numLateTasks = 0;
    int numFutureTasks = 0;
    long maxTaskLag = 0;

    for (SingularityPendingTaskId pendingTaskId : pendingTaskIds) {
      if (pendingTaskId.getNextRunAt() <= now) {
        numLateTasks++;
      } else {
        numFutureTasks++;
      }

      if (now - pendingTaskId.getNextRunAt() > maxTaskLag) {
        maxTaskLag = now - pendingTaskId.getNextRunAt();
      }
    }

    return new SingularityScheduledTasksInfo(numLateTasks, numFutureTasks, maxTaskLag);
  }
}
