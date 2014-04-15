package com.hubspot.singularity.executor;

import com.google.common.util.concurrent.ListenableFuture;

public class SingularityExecutorTaskHolder {

  private final SingularityExecutorTask task;
  private final ListenableFuture<Integer> future;
  
  public SingularityExecutorTaskHolder(SingularityExecutorTask task, ListenableFuture<Integer> future) {
    this.task = task;
    this.future = future;
  }

  public SingularityExecutorTask getTask() {
    return task;
  }

  public ListenableFuture<Integer> getFuture() {
    return future;
  }

  @Override
  public String toString() {
    return "SingularityExecutorTaskHolder [task=" + task + ", future=" + future + "]";
  }
  
}
