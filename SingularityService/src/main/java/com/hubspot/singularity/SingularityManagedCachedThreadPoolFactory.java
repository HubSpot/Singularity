package com.hubspot.singularity;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.config.SingularityConfiguration;

@Singleton
public class SingularityManagedCachedThreadPoolFactory {
  private final AtomicBoolean stopped = new AtomicBoolean();
  private final List<ExecutorService> executorPools = new ArrayList<>();

  private final long timeoutInMillis;

  @Inject
  public SingularityManagedCachedThreadPoolFactory(final SingularityConfiguration configuration) {
    this.timeoutInMillis = TimeUnit.SECONDS.toMillis(configuration.getThreadpoolShutdownDelayInSeconds());
  }

  public synchronized ExecutorService get(String name) {
    checkState(!stopped.get(), "already stopped");
    ExecutorService service = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat(name + "-%d").build());
    executorPools.add(service);
    return service;
  }

  public void stop() throws Exception {
    if (!stopped.getAndSet(true)) {
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
