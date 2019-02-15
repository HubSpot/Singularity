package com.hubspot.singularity;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.config.SingularityConfiguration;

@Singleton
public class SingularityManagedScheduledExecutorServiceFactory {

  private final AtomicBoolean stopped = new AtomicBoolean();
  private final List<ScheduledExecutorService> executorPools = new ArrayList<>();

  private final long timeoutInMillis;

  @Inject
  public SingularityManagedScheduledExecutorServiceFactory(final SingularityConfiguration configuration) {
    this.timeoutInMillis = TimeUnit.SECONDS.toMillis(configuration.getThreadpoolShutdownDelayInSeconds());
  }

  public ScheduledExecutorService get(String name) {
    return get(name, 1);
  }

  public synchronized ScheduledExecutorService get(String name, int poolSize) {
    checkState(!stopped.get(), "already stopped");
    ScheduledExecutorService service = Executors.newScheduledThreadPool(poolSize, new ThreadFactoryBuilder().setNameFormat(name + "-%d").setDaemon(true).build());
    executorPools.add(service);
    return service;
  }

  public void stop() throws Exception {
    if (!stopped.getAndSet(true)) {
      for (ScheduledExecutorService service : executorPools) {
        service.shutdown();
      }

      long timeoutLeftInMillis = timeoutInMillis;

      for (ScheduledExecutorService service : executorPools) {
        final long start = System.currentTimeMillis();

        if (!service.awaitTermination(timeoutLeftInMillis, TimeUnit.MILLISECONDS)) {
          return;
        }

        timeoutLeftInMillis -= (System.currentTimeMillis() - start);
      }
    }
  }
}
