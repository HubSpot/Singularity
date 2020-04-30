package com.hubspot.singularity;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.async.ExecutorAndQueue;
import com.hubspot.singularity.config.SingularityConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
public class SingularityManagedThreadPoolFactory {
  private final AtomicBoolean stopped = new AtomicBoolean();
  private final List<ExecutorService> executorPools = new ArrayList<>();

  private final long timeoutInMillis;

  @Inject
  public SingularityManagedThreadPoolFactory(
    final SingularityConfiguration configuration
  ) {
    this.timeoutInMillis =
      TimeUnit.SECONDS.toMillis(configuration.getThreadpoolShutdownDelayInSeconds());
  }

  public synchronized ExecutorService get(String name) {
    checkState(!stopped.get(), "already stopped");
    ExecutorService service = Executors.newCachedThreadPool(
      new ThreadFactoryBuilder().setNameFormat(name + "-%d").build()
    );
    executorPools.add(service);
    return service;
  }

  public synchronized ExecutorService get(String name, int maxSize) {
    checkState(!stopped.get(), "already stopped");
    ExecutorService service = Executors.newFixedThreadPool(
      maxSize,
      new ThreadFactoryBuilder().setNameFormat(name + "-%d").build()
    );
    executorPools.add(service);
    return service;
  }

  public synchronized ExecutorAndQueue get(String name, int maxSize, int queueSize) {
    checkState(!stopped.get(), "already stopped");
    LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(queueSize);
    ExecutorService service = new ThreadPoolExecutor(
      maxSize,
      maxSize,
      0L,
      TimeUnit.MILLISECONDS,
      queue,
      new ThreadFactoryBuilder().setNameFormat(name + "-%d").build()
    );
    executorPools.add(service);
    return new ExecutorAndQueue(service, queue, queueSize);
  }

  public synchronized ExecutorService getSingleThreaded(String name) {
    checkState(!stopped.get(), "already stopped");
    ExecutorService service = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat(name + "-%d").build()
    );
    executorPools.add(service);
    return service;
  }

  public void stop() throws Exception {
    if (!stopped.getAndSet(true)) {
      if (timeoutInMillis == 0) {
        executorPools.forEach(ExecutorService::shutdownNow);
      } else {
        executorPools.forEach(ExecutorService::shutdown);

        long timeoutLeftInMillis = timeoutInMillis;

        for (ExecutorService service : executorPools) {
          final long start = System.currentTimeMillis();

          if (!service.awaitTermination(timeoutLeftInMillis, TimeUnit.MILLISECONDS)) {
            return;
          }

          timeoutLeftInMillis -= (System.currentTimeMillis() - start);
        }
      }
    }
  }
}
