package com.hubspot.singularity.async;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;

public class CompletableFutures {
  private CompletableFutures() {}

  public static <T> CompletableFuture<Void> allOf(Collection<CompletableFuture<T>> futures) {
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
  }

  public static <T> CompletableFuture<Object> anyOf(Collection<CompletableFuture<T>> futures) {
    return CompletableFuture.anyOf(futures.toArray(new CompletableFuture[futures.size()]));
  }

  public static <T> CompletableFuture<T> exceptionalFuture(Throwable t) {
    CompletableFuture<T> future = new CompletableFuture<>();
    future.completeExceptionally(t);
    return future;
  }

  /**
   * Return a future that completes with a timeout after a delay.
   */
  public static CompletableFuture<Timeout> timeoutFuture(HashedWheelTimer hwt, long delay, TimeUnit timeUnit) {
    try {
      CompletableFuture<Timeout> future = new CompletableFuture<>();
      hwt.newTimeout(future::complete, delay, timeUnit);
      return future;
    } catch (Throwable t) {
      return exceptionalFuture(t);
    }
  }

  /**
   * Useful for composing an async call in response to exceptional cases.
   * For example:
   * {@code
   *    return thenHandleCompose(myFuture, (success, th) -> {
   *        if (th != null) {
   *          return newCompletableFuture();
   *        } else {
   *          return CompletableFuture.completedFuture(success);
   *        }
   *    });
   * }
   */
  public static <T, U> CompletableFuture<U> thenHandleCompose(CompletionStage<T> completionStage,
                                                              BiFunction<? super T, Throwable, ? extends CompletionStage<U>> fn) {
    return completionStage.handle((success, throwable) -> new SuccessOrThrowable<>(success, throwable))
        .thenCompose(pair ->
            fn.apply(pair.item, pair.ex)
        ).toCompletableFuture();
  }

  public static <T, U> CompletableFuture<U> thenHandleComposeAsync(CompletionStage<T> completionStage,
                                                                   BiFunction<? super T, Throwable, ? extends CompletionStage<U>> fn,
                                                                   ExecutorService executorService) {
    return completionStage.handleAsync((success, throwable) -> new SuccessOrThrowable<>(success, throwable), executorService)
        .thenComposeAsync(
            pair -> fn.apply(pair.item, pair.ex),
            executorService
        ).toCompletableFuture();
  }

  private static final HashedWheelTimer DEFAULT_TIMER = new HashedWheelTimer(
      new ThreadFactoryBuilder().setDaemon(true).setNameFormat("futures-utils-%s").build()
  );

  public static <T> CompletableFuture<T> enforceTimeout(CompletionStage<T> underlyingFuture,
                                                        long timeout,
                                                        TimeUnit timeUnit) {
    return enforceTimeout(underlyingFuture, DEFAULT_TIMER, timeout, timeUnit);
  }

  public static <T> CompletableFuture<T> enforceTimeout(CompletionStage<T> underlyingFuture,
                                                        long timeout,
                                                        TimeUnit timeUnit,
                                                        Supplier<Exception> exceptionSupplier) {
    return enforceTimeout(underlyingFuture, DEFAULT_TIMER, timeout, timeUnit, exceptionSupplier);
  }

  public static <T> CompletableFuture<T> enforceTimeout(CompletionStage<T> underlyingFuture,
                                                        HashedWheelTimer timer,
                                                        long timeout,
                                                        TimeUnit timeUnit) {
    return enforceTimeout(underlyingFuture, timer, timeout, timeUnit, TimeoutException::new);
  }

  public static <T> CompletableFuture<T> enforceTimeout(CompletionStage<T> underlyingFuture,
                                                        HashedWheelTimer timer,
                                                        long timeout,
                                                        TimeUnit timeUnit,
                                                        Supplier<Exception> exceptionSupplier) {
    // We don't want to muck with the underlying future passed in, so
    // chaining a .thenApply(x -> x) forces a new future to be created with its own
    // completion tracking. In this way, the original future is left alone and can
    // time out on its own schedule.
    CompletableFuture<T> future = underlyingFuture.thenApply(x -> x)
        .toCompletableFuture();
    Timeout hwtTimeout = timer.newTimeout(
        (ignored) -> future.completeExceptionally(exceptionSupplier.get()),
        timeout,
        timeUnit
    );
    future.whenComplete((result, throwable) -> hwtTimeout.cancel());
    return future;
  }

  public static <T> CompletableFuture<T> executeWithTimeout(Callable<T> callable,
                                                            ExecutorService executorService,
                                                            long timeout,
                                                            TimeUnit timeUnit) {
    return executeWithTimeout(callable, executorService, DEFAULT_TIMER, timeout, timeUnit);
  }

  public static <T> CompletableFuture<T> executeWithTimeout(Callable<T> callable,
                                                            ExecutorService executorService,
                                                            HashedWheelTimer timer,
                                                            long timeout,
                                                            TimeUnit timeUnit) {
    CompletableFuture<T> future = new CompletableFuture<>();
    AtomicReference<Timeout> timeoutRef = new AtomicReference<>();
    Future<Void> underlying = executorService.submit(() -> {
      if (future.complete(callable.call())) {
        Timeout timeout1 = timeoutRef.get();
        if (timeout1 != null) {
          timeout1.cancel();
        }
      }
      return null;
    });

    timeoutRef.set(timer.newTimeout((ignored) -> {
      if (!future.isDone()) {
        if (future.completeExceptionally(new TimeoutException())) {
          underlying.cancel(true);
        }
      }
    }, timeout, timeUnit));
    return future;
  }

  public static <T> T join(CompletableFuture<T> future, long timeout, TimeUnit unit) {
    try {
      return future.get(timeout, unit);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> Collector<CompletableFuture<T>, ?, CompletableFuture<Void>> collectAllOf() {
    return Collectors.collectingAndThen(
        Collectors.toList(),
        CompletableFutures::allOf);
  }

  private static class SuccessOrThrowable<T> {
    private final T item;
    private final Throwable ex;

    public SuccessOrThrowable(T item, Throwable ex) {
      this.item = item;
      this.ex = ex;
    }
  }
}
