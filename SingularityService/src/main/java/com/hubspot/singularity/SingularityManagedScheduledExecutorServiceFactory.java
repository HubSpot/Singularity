package com.hubspot.singularity;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.config.SingularityConfiguration;
import java.util.HashMap;
import java.util.Map;
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
  private final AtomicBoolean leaderStopped = new AtomicBoolean();
  private final Map<String, ScheduledExecutorService> executorPools = new HashMap<>();
  private final Map<String, ScheduledExecutorService> leaderPollerPools = new HashMap<>();

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
      leaderPollerPools.put(name, service);
    } else {
      executorPools.put(name, service);
    }
    return service;
  }

  public void stopLeaderPollers() throws Exception {
    if (!leaderStopped.getAndSet(true)) {
      long timeoutLeftInMillis = timeoutInMillis;
      for (Map.Entry<String, ScheduledExecutorService> entry : leaderPollerPools.entrySet()) {
        final long start = System.currentTimeMillis();
        closeExecutor(entry.getValue(), timeoutLeftInMillis, entry.getKey());
        timeoutLeftInMillis -= (System.currentTimeMillis() - start);
      }
      for (Map.Entry<String, ScheduledExecutorService> entry : executorPools.entrySet()) {
        final long start = System.currentTimeMillis();
        closeExecutor(entry.getValue(), timeoutLeftInMillis, entry.getKey());
        timeoutLeftInMillis -= (System.currentTimeMillis() - start);
      }
    }
  }

  public void stopOtherPollers() throws Exception {
    if (!stopped.getAndSet(true)) {
      long timeoutLeftInMillis = timeoutInMillis;
      for (Map.Entry<String, ScheduledExecutorService> entry : leaderPollerPools.entrySet()) {
        final long start = System.currentTimeMillis();
        closeExecutor(entry.getValue(), timeoutLeftInMillis, entry.getKey());
        timeoutLeftInMillis -= (System.currentTimeMillis() - start);
      }
      for (Map.Entry<String, ScheduledExecutorService> entry : executorPools.entrySet()) {
        final long start = System.currentTimeMillis();
        closeExecutor(entry.getValue(), timeoutLeftInMillis, entry.getKey());
        timeoutLeftInMillis -= (System.currentTimeMillis() - start);
      }
    }
  }

  public void closeExecutor(
    ScheduledExecutorService scheduledExecutorService,
    long timeoutInMillis,
    String name
  ) {
    // This is basically stolen from the ExecutorService javadoc with some slight modifications
    if (!scheduledExecutorService.isTerminated()) {
      scheduledExecutorService.shutdown();
      try {
        if (
          !scheduledExecutorService.awaitTermination(
            timeoutInMillis,
            TimeUnit.MILLISECONDS
          )
        ) {
          scheduledExecutorService.shutdownNow();
          if (
            !scheduledExecutorService.awaitTermination(
              timeoutInMillis,
              TimeUnit.MILLISECONDS
            )
          ) {
            LOG.error(
              "{}: Tasks in executor failed to terminate in time, continuing with shutdown regardless.",
              name
            );
          }
        }
      } catch (InterruptedException ie) {
        scheduledExecutorService.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }
}
