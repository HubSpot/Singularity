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
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SingularityManagedThreadPoolFactory {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityManagedThreadPoolFactory.class
  );

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

  public synchronized ExecutorService get(String name, int size) {
    checkState(!stopped.get(), "already stopped");
    ExecutorService service = new ThreadPoolExecutor(
      size,
      size,
      60L,
      TimeUnit.SECONDS,
      new LinkedBlockingQueue<>(),
      new ThreadFactoryBuilder().setNameFormat(name + "-%d").build()
    );
    executorPools.add(service);
    return service;
  }

  public synchronized ExecutorAndQueue get(
    String name,
    int maxSize,
    int queueSize,
    boolean blockWhenFull
  ) {
    checkState(!stopped.get(), "already stopped");
    LinkedBlockingQueue<Runnable> queue = blockWhenFull
      ? new ThreadPoolQueue(queueSize)
      : new LinkedBlockingQueue<>(queueSize);
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
            LOG.warn("Executor service tasks did not exit in time");
            continue;
          }

          timeoutLeftInMillis -= (System.currentTimeMillis() - start);
        }
      }
    }
  }

  public static final class ThreadPoolQueue extends LinkedBlockingQueue<Runnable> {

    public ThreadPoolQueue(int capacity) {
      super(capacity);
    }

    @Override
    public boolean offer(Runnable e) {
      try {
        put(e);
      } catch (InterruptedException e1) {
        return false;
      }
      return true;
    }
  }
}
