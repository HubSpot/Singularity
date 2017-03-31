package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DisasterManager;
import com.hubspot.singularity.mesos.SingularityMesosModule;

@Singleton
public class SingularityCleanupPoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityCleanupPoller.class);

  private final SingularityCleaner cleaner;
  private final DisasterManager disasterManager;

  @Inject
  SingularityCleanupPoller(SingularityConfiguration configuration, SingularityCleaner cleaner, @Named(SingularityMesosModule.SCHEDULER_LOCK_NAME) final Lock lock, DisasterManager disasterManager) {
    super(configuration.getCleanupEverySeconds(), TimeUnit.SECONDS, lock);

    this.cleaner = cleaner;
    this.disasterManager = disasterManager;
  }

  @Override
  public void runActionOnPoll() {
    if (!disasterManager.isDisabled(SingularityAction.RUN_CLEANUP_POLLER)) {
      cleaner.drainCleanupQueue();
    } else {
      LOG.warn("Cleanup poller is currently disabled");
    }
  }
}
