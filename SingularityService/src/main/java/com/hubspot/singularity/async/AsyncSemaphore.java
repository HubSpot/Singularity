package com.hubspot.singularity.async;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * AsyncSemaphore guarantees that at most N executions
 * of an underlying completablefuture exeuction are occuring
 * at the same time.
 *
 * The general strategy is to try acquiring a permit for execution.
 * If it succeeds, the semaphore just proceeds normally. Otherwise,
 * it pushes the execution onto a queue.
 *
 * At the completion of any execution, the queue is checked for
 * any pending executions. If any executions are found, they are
 * executed in order.
 *
 * @param <T>
 */
public class AsyncSemaphore<T> {
  private final StampedLock stampedLock = new StampedLock();
  private final AtomicInteger concurrentRequests = new AtomicInteger();
  private final Queue<DelayedExecution<T>> requestQueue;
  private final com.google.common.base.Supplier<Integer> queueRejectionThreshold;
  private final Supplier<Exception> timeoutExceptionSupplier;
  private final PermitSource permitSource;
  private final ScheduledExecutorService flushingExecutor = Executors.newScheduledThreadPool(5,
      new ThreadFactoryBuilder().setDaemon(true).setNameFormat("async-semaphore-flush-pool- %d").build());;

  /**
   * Create an AsyncSemaphore with the given limit.
   *
   * @param concurrentRequestLimit - A supplier saying how many concurrent requests are allowed
   */
  public static AsyncSemaphoreBuilder newBuilder(Supplier<Integer> concurrentRequestLimit) {
    return new AsyncSemaphoreBuilder(new PermitSource(concurrentRequestLimit));
  }

  /**
   * Create an AsyncSemaphore with the given permit source.
   *
   * @param permitSource - A source for the permits used by the semaphore
   */
  public static AsyncSemaphoreBuilder newBuilder(PermitSource permitSource) {
    return new AsyncSemaphoreBuilder(permitSource);
  }

  AsyncSemaphore(PermitSource permitSource,
                 Queue<DelayedExecution<T>> requestQueue,
                 Supplier<Integer> queueRejectionThreshold,
                 Supplier<Exception> timeoutExceptionSupplier,
                 boolean flushQueuePeriodically) {
    this.permitSource = permitSource;
    this.requestQueue = requestQueue;
    this.queueRejectionThreshold = Suppliers.memoizeWithExpiration(queueRejectionThreshold::get, 1, TimeUnit.MINUTES);
    this.timeoutExceptionSupplier = timeoutExceptionSupplier;
    if (flushQueuePeriodically) {
      flushingExecutor.scheduleAtFixedRate(() -> flushQueue(), 1, 1, TimeUnit.SECONDS);
    }
  }

  public CompletableFuture<T> call(Callable<CompletableFuture<T>> execution) {
    return callWithQueueTimeout(execution, Optional.empty());
  }

  /**
   * Try to execute the supplier if there are enough permits available.
   * If not, add the execution to a queue (if there is room).
   * If the queue attempts to start the execution after the timeout
   * has passed, short circuit the execution and complete the future
   * exceptionally with TimeoutException
   *
   * @param execution - The execution of the item
   * @param timeout - The time before which we'll short circuit the execution
   * @param timeUnit
   * @return
   */
  public CompletableFuture<T> callWithQueueTimeout(Callable<CompletableFuture<T>> execution, long timeout, TimeUnit timeUnit) {
    return callWithQueueTimeout(execution, Optional.of(TimeUnit.MILLISECONDS.convert(timeout, timeUnit)));
  }

  private CompletableFuture<T> callWithQueueTimeout(Callable<CompletableFuture<T>> execution,
                                                    Optional<Long> timeoutInMillis) {

    if (timeoutInMillis.isPresent() && timeoutInMillis.get() <= 0) {
      return CompletableFutures.exceptionalFuture(timeoutExceptionSupplier.get());

    } else if (tryAcquirePermit()) {
      CompletableFuture<T> responseFuture = executeCall(execution);
      pollQueueOnCompletion(responseFuture);
      return responseFuture;

    } else {
      DelayedExecution<T> delayedExecution = new DelayedExecution<>(execution, timeoutExceptionSupplier, timeoutInMillis);
      if (!tryEnqueueAttempt(delayedExecution)) {
        return CompletableFutures.exceptionalFuture(
            new RejectedExecutionException("Could not queue future for execution.")
        );
      }
      return delayedExecution.getResponseFuture();
    }
  }

  private <U> void pollQueueOnCompletion(CompletableFuture<U> future) {
    future.whenComplete((ignored1, ignored2) -> {

      // iterate through expired executions rather than using callbacks
      // to avoid StackoverflowError if futures are completed or expired
      while (true) {
        DelayedExecution<T> nextExecutionDue = requestQueue.poll();

        if (nextExecutionDue == null) {
          releasePermit();
          return;

        } else if (nextExecutionDue.isExpired()) {
          nextExecutionDue.getResponseFuture().completeExceptionally(timeoutExceptionSupplier.get());

        } else {
          // reuse the previous permit for the queued request
          CompletableFuture<Void> nextExecution = nextExecutionDue.execute();

          if (!nextExecution.isDone()) {
            pollQueueOnCompletion(nextExecution);
            return;
          }
        }
      }
    });
  }

  private boolean tryAcquirePermit() {
    boolean acquired = permitSource.tryAcquire();

    if (acquired) {
      concurrentRequests.incrementAndGet();
    }

    return acquired;
  }

  private int releasePermit() {
    permitSource.release();
    return concurrentRequests.decrementAndGet();
  }

  private static <T> CompletableFuture<T> executeCall(Callable<CompletableFuture<T>> execution) {
    try {
      return execution.call();
    } catch (Throwable t) {
      return CompletableFutures.exceptionalFuture(t);
    }
  }

  /**
   * enqueue the attempt into our underlying queue. since it's expensive to dynamically
   * resize the queue, we have a separate rejection threshold which, if less than 0 is
   * ignored, but otherwise is the practical cap on the size of the queue.
   */
  private boolean tryEnqueueAttempt(DelayedExecution<T> delayedExecution) {
    int rejectionThreshold = queueRejectionThreshold.get();
    if (rejectionThreshold < 0) {
      return requestQueue.offer(delayedExecution);
    }
    long stamp = stampedLock.readLock();
    try {
      while (requestQueue.size() < rejectionThreshold) {
        long writeStamp = stampedLock.tryConvertToWriteLock(stamp);
        if (writeStamp != 0L) {
          stamp = writeStamp;
          return requestQueue.offer(delayedExecution);
        } else {
          stampedLock.unlock(stamp);
          stamp = stampedLock.writeLock();
        }
      }
      return false;
    } finally {
      stampedLock.unlock(stamp);
    }
  }

  private void  flushQueue() {
    if (tryAcquirePermit()) {
      // Pass in an already completed future so that we execute the callback on this thread
      pollQueueOnCompletion(CompletableFuture.completedFuture(true));
    }
  }

  static class DelayedExecution<T> {
    private static final AtomicIntegerFieldUpdater<DelayedExecution> EXECUTED_UPDATER = AtomicIntegerFieldUpdater.newUpdater(
        DelayedExecution.class,
        "executed"
    );
    private final Callable<CompletableFuture<T>> execution;
    private final CompletableFuture<T> responseFuture;
    private final Supplier<Exception> timeoutExceptionSupplier;
    private final long deadlineEpochMillis;
    @SuppressWarnings( "unused" ) // use the EXECUTED_UPDATER
    private volatile int executed = 0;

    private DelayedExecution(Callable<CompletableFuture<T>> execution,
                             Supplier<Exception> timeoutExceptionSupplier,
                             Optional<Long> timeoutMillis) {
      this.execution = execution;
      this.responseFuture = new CompletableFuture<>();
      this.timeoutExceptionSupplier = timeoutExceptionSupplier;
      this.deadlineEpochMillis = timeoutMillis.map(x -> System.currentTimeMillis() + x).orElse(0L);
    }

    private CompletableFuture<T> getResponseFuture() {
      return responseFuture;
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    private CompletableFuture<Void> execute() {
      if (!EXECUTED_UPDATER.compareAndSet(this, 0, 1)) {
        return CompletableFuture.completedFuture(null);
      }

      return executeCall(execution).whenComplete((response, ex) -> {
        if (ex == null) {
          responseFuture.complete(response);
        } else {
          responseFuture.completeExceptionally(ex);
        }
      }).thenApply(ignored -> null);
    }

    private boolean isExpired() {
      return deadlineEpochMillis > 0 && System.currentTimeMillis() > deadlineEpochMillis;
    }
  }

  public int getQueueSize() {
    long stamp = stampedLock.tryOptimisticRead();
    int queueSize = requestQueue.size();
    if (!stampedLock.validate(stamp)) {
      stamp = stampedLock.readLock();
      try {
        queueSize = requestQueue.size();
      } finally {
        stampedLock.unlockRead(stamp);
      }
    }
    return queueSize;
  }

  public int getConcurrentRequests() {
    return concurrentRequests.get();
  }
}
