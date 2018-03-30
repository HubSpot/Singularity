package com.hubspot.singularity.async;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class AsyncSemaphoreBuilder {
  private final PermitSource permitSource;

  private int queueSize = -1;
  private Supplier<Integer> queueRejectionThreshold = () -> -1;
  private Supplier<Exception> timeoutExceptionSupplier = TimeoutException::new;
  private boolean flushQueuePeriodically = false;

  AsyncSemaphoreBuilder(PermitSource permitSource) {
    this.permitSource = permitSource;
  }

  /**
   * Sets the maximum size of the queue. Note that this should be larger than any
   * desired queueRejectionThreshold.
   */
  public AsyncSemaphoreBuilder withQueueSize(int queueSize) {
    this.queueSize = queueSize;
    return this;
  }

  /**
   * Sets a dynamic rejection threshold. If -1, the queue size is used
   * to reject requests. Otherwise, this number is the effective
   * number of allowed tasks.
   */
  public AsyncSemaphoreBuilder withQueueRejectionThreshold(Supplier<Integer> queueRejectionThreshold) {
    this.queueRejectionThreshold = queueRejectionThreshold;
    return this;
  }

  /**
   * Sets the type of the exception to be thrown when the {@code callWithQueueTimeout}
   * method is called and the call is queued for longer than the timeout.
   */
  public AsyncSemaphoreBuilder withTimeoutExceptionSupplier(Supplier<Exception> timeoutExceptionSupplier) {
    this.timeoutExceptionSupplier = timeoutExceptionSupplier;
    return this;
  }

  /**
   * If set to a positive value, will flush the internal queue every once a second
   * used in cases where the async semaphore is used in batch processing to avoid a
   * rare case when work can become stuck in the queue and never complete.
   * @param flushQueuePeriodically
   * @return
   */
  public AsyncSemaphoreBuilder setFlushQueuePeriodically(boolean flushQueuePeriodically) {
    this.flushQueuePeriodically = flushQueuePeriodically;
    return this;
  }

  public <T> AsyncSemaphore<T> build() {
    return new AsyncSemaphore<>(
        permitSource,
        queueSize == -1 ? new ConcurrentLinkedQueue<>() : new ArrayBlockingQueue<>(queueSize),
        queueRejectionThreshold,
        timeoutExceptionSupplier,
        flushQueuePeriodically
    );
  }
}
