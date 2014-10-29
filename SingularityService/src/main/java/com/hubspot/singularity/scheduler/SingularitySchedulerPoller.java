package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.mesos.SingularityMesosSchedulerDelegator;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

@Singleton
public class SingularitySchedulerPoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularitySchedulerPoller.class);

  private final SingularityScheduler scheduler;
  private final Provider<SingularitySchedulerStateCache> stateCacheProvider;

  @Inject
  public SingularitySchedulerPoller(final LeaderLatch leaderLatch, final SingularityMesosSchedulerDelegator mesosScheduler, SingularityExceptionNotifier exceptionNotifier,
      SingularityScheduler scheduler, SingularityConfiguration configuration, SingularityAbort abort, Provider<SingularitySchedulerStateCache> stateCacheProvider) {
    super(leaderLatch, mesosScheduler, exceptionNotifier, abort, configuration.getCheckSchedulerEverySeconds(), TimeUnit.SECONDS, SchedulerLockType.LOCK);

    this.stateCacheProvider = stateCacheProvider;
    this.scheduler = scheduler;
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
