package com.hubspot.singularity;

import java.util.ArrayList;
import java.util.List;

public class SingularityScheduledTasksInfo {

  private final int numLateTasks;
  private final int numFutureTasks;
  private final long maxTaskLag;
  private final long timestamp;
  private final List<SingularityPendingTask> lateTasks;

  private SingularityScheduledTasksInfo(int numLateTasks, int numFutureTasks, long maxTaskLag, long timestamp, List<SingularityPendingTask> lateTasks) {
    this.numLateTasks = numLateTasks;
    this.numFutureTasks = numFutureTasks;
    this.maxTaskLag = maxTaskLag;
    this.timestamp = timestamp;
    this.lateTasks = lateTasks;
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

  public List<SingularityPendingTask> getLateTasks() {
    return lateTasks;
  }

  public static SingularityScheduledTasksInfo getInfo(List<SingularityPendingTask> pendingTasks, long millisDeltaForLateTasks) {
    final long now = System.currentTimeMillis();

    int numLateTasks = 0;
    int numFutureTasks = 0;
    long maxTaskLag = 0;
    List<SingularityPendingTask> lateTasks = new ArrayList<>();

    for (SingularityPendingTask pendingTask : pendingTasks) {
      long delta = now - pendingTask.getPendingTaskId().getNextRunAt();

      if (delta > millisDeltaForLateTasks) {
        numLateTasks++;
        lateTasks.add(pendingTask);
      } else {
        numFutureTasks++;
      }

      if (delta > maxTaskLag) {
        maxTaskLag = delta;
      }
    }

    return new SingularityScheduledTasksInfo(numLateTasks, numFutureTasks, maxTaskLag, now, lateTasks);
  }
}
