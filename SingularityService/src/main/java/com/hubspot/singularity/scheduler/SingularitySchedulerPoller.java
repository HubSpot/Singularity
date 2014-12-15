package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.mesos.SingularityMesosModule;

@Singleton
public class SingularitySchedulerPoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularitySchedulerPoller.class);

  private final SingularityScheduler scheduler;
  private final Provider<SingularitySchedulerStateCache> stateCacheProvider;

  @Inject
  SingularitySchedulerPoller(Provider<SingularitySchedulerStateCache> stateCacheProvider, SingularityScheduler scheduler,
      SingularityConfiguration configuration, @Named(SingularityMesosModule.SCHEDULER_LOCK_NAME) final Lock lock) {
    super(configuration.getCheckSchedulerEverySeconds(), TimeUnit.SECONDS);

    this.stateCacheProvider = stateCacheProvider;
    this.scheduler = scheduler;
    this.lockHolder = Optional.of(lock);
  }

  @Override
  public void runActionOnPoll() {
    final long start = System.currentTimeMillis();

    final SingularitySchedulerStateCache stateCache = stateCacheProvider.get();

    scheduler.checkForDecomissions(stateCache);
    scheduler.drainPendingQueue(stateCache);

    LOG.info("Processed decomissions and pending queue in {}", JavaUtils.duration(start));
  }
}
