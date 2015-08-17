package com.hubspot.singularity;

import static com.google.common.base.Preconditions.checkState;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Provider;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.dropwizard.lifecycle.Managed;

public class SingularityManagedScheduledExecutorServiceProvider implements Provider<ScheduledExecutorService>, Managed {

  private final AtomicBoolean stopped = new AtomicBoolean();
  private ScheduledExecutorService service;

  private final long timeoutInSeconds;

  public SingularityManagedScheduledExecutorServiceProvider(final int poolSize, final long timeoutInSeconds, final String name) {
    this.service = Executors.newScheduledThreadPool(poolSize, new ThreadFactoryBuilder().setNameFormat(name + "-pool-%d").setDaemon(true).build());
    this.timeoutInSeconds = timeoutInSeconds;
  }

  @Override
  public synchronized ScheduledExecutorService get() {
    checkState(!stopped.get(), "already stopped");
    return service;
  }

  @Override
  public void start() throws Exception {
    // Ignored
  }

  @Override
  public void stop() throws Exception {
    if (!stopped.getAndSet(true)) {
      service.shutdown();
      service.awaitTermination(timeoutInSeconds, TimeUnit.SECONDS);
    }
  }
}
