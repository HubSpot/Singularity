package com.hubspot.singularity;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.config.SingularityConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SingularityManagedScheduledExecutorServiceFactory {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityManagedScheduledExecutorServiceFactory.class
  );

  private final AtomicBoolean stopped = new AtomicBoolean();
  private final List<ScheduledExecutorService> executorPools = new ArrayList<>();

  private final long timeoutInMillis;

  @Inject
  public SingularityManagedScheduledExecutorServiceFactory(
    final SingularityConfiguration configuration
  ) {
    this.timeoutInMillis =
      TimeUnit.SECONDS.toMillis(configuration.getThreadpoolShutdownDelayInSeconds());
  }

  public ScheduledExecutorService get(String name) {
    return get(name, 1, false);
  }

  public ScheduledExecutorService get(String name, boolean isLeaderOnlyPoller) {
    return get(name, 1, isLeaderOnlyPoller);
  }

  public ScheduledExecutorService get(String name, int poolSize) {
    return get(name, poolSize, false);
  }

  public synchronized ScheduledExecutorService get(
    String name,
    int poolSize,
    boolean isLeaderOnlyPoller
  ) {
    checkState(!stopped.get(), "already stopped");
    ScheduledExecutorService service = Executors.newScheduledThreadPool(
      poolSize,
      new ThreadFactoryBuilder().setNameFormat(name + "-%d").setDaemon(true).build()
    );
    if (isLeaderOnlyPoller) {
      executorPools.add(0, service);
    } else {
      executorPools.add(service);
    }
    return service;
  }

  public void stop() throws Exception {
    if (!stopped.getAndSet(true)) {
      executorPools.forEach(ScheduledExecutorService::shutdown);

      long timeoutLeftInMillis = timeoutInMillis;

      for (ScheduledExecutorService service : executorPools) {
        final long start = System.currentTimeMillis();

        if (!service.awaitTermination(timeoutLeftInMillis, TimeUnit.MILLISECONDS)) {
          LOG.warn("Scheduled executor service task did not exit cleanly");
          service.shutdownNow();
          continue;
        }

        timeoutLeftInMillis -= (System.currentTimeMillis() - start);
      }
    }
  }
}
