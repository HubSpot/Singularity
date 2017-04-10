package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DisasterManager;
import com.hubspot.singularity.mesos.SingularitySchedulerLock;

@Singleton
public class SingularityCleanupPoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityCleanupPoller.class);

  private final SingularityCleaner cleaner;
  private final DisasterManager disasterManager;
  private final int delayCleanupWhenAboveTasks;
  private final long delayCleanupForMs;

  private AtomicLong nextRunAfter = new AtomicLong(0);

  @Inject
  SingularityCleanupPoller(SingularityConfiguration configuration, SingularityCleaner cleaner, SingularitySchedulerLock lock, DisasterManager disasterManager) {
    super(configuration.getCleanupEverySeconds(), TimeUnit.SECONDS, lock);

    this.cleaner = cleaner;
    this.disasterManager = disasterManager;
    this.delayCleanupWhenAboveTasks = configuration.getDelayCleanupWhenAboveTasks();
    this.delayCleanupForMs = configuration.getDelayCleanupForMsWhenAboveTasks();
  }

  @Override
  public void runActionOnPoll() {
    if (!disasterManager.isDisabled(SingularityAction.RUN_CLEANUP_POLLER)) {
      int lastCleanupCount = cleaner.drainCleanupQueue();
      if (lastCleanupCount > delayCleanupWhenAboveTasks) {
        LOG.warn("Last run had {} cleanup tasks (> {}), delaying cleanup poller for {}ms", lastCleanupCount, delayCleanupWhenAboveTasks, delayCleanupForMs);
        nextRunAfter.set(System.currentTimeMillis() + delayCleanupForMs);
      }
    } else {
      LOG.warn("Cleanup poller is currently disabled");
    }
  }

  @Override
  protected long getNextRunAfterTime() {
    return nextRunAfter.get();
  }
}
