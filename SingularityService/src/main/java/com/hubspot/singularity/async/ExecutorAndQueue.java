package com.hubspot.singularity.async;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

public class ExecutorAndQueue {
  private final ExecutorService executorService;
  private final LinkedBlockingQueue<Runnable> queue;
  private final int queueLimit;

  public ExecutorAndQueue(ExecutorService executorService, LinkedBlockingQueue<Runnable> queue, int queueLimit) {
    this.executorService = executorService;
    this.queue = queue;
    this.queueLimit = queueLimit;
  }

  public ExecutorService getExecutorService() {
    return executorService;
  }

  public LinkedBlockingQueue<Runnable> getQueue() {
    return queue;
  }

  public int getQueueLimit() {
    return queueLimit;
  }
}
