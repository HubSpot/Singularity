package com.hubspot.singularity.scheduler;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.history.SingularityHistoryPurger;
import com.hubspot.singularity.mesos.OfferCache;
import com.hubspot.singularity.mesos.SingularityNoOfferCache;
import com.hubspot.singularity.mesos.SingularityOfferCache;

public class SingularitySchedulerModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(SingularityHealthchecker.class).in(Scopes.SINGLETON);
    bind(SingularityNewTaskChecker.class).in(Scopes.SINGLETON);
    bind(SingularityCleanupPoller.class).in(Scopes.SINGLETON);
    bind(SingularityExpiringUserActionPoller.class).in(Scopes.SINGLETON);
    bind(SingularityHistoryPurger.class).in(Scopes.SINGLETON);
    bind(SingularitySlaveReconciliationPoller.class).in(Scopes.SINGLETON);
    bind(SingularityCooldownPoller.class).in(Scopes.SINGLETON);
    bind(SingularityDeployPoller.class).in(Scopes.SINGLETON);
    bind(SingularityCooldownPoller.class).in(Scopes.SINGLETON);
    bind(SingularityDeployPoller.class).in(Scopes.SINGLETON);
    bind(SingularitySchedulerPoller.class).in(Scopes.SINGLETON);
    bind(SingularityJobPoller.class).in(Scopes.SINGLETON);
    bind(SingularityTaskShellCommandDispatchPoller.class).in(Scopes.SINGLETON);
    bind(SingularityTaskReconciliationPoller.class).in(Scopes.SINGLETON);
    bind(SingularityScheduler.class).in(Scopes.SINGLETON);
    bind(SingularityCooldownChecker.class).in(Scopes.SINGLETON);
    bind(SingularityDeployChecker.class).in(Scopes.SINGLETON);
    bind(SingularityCleaner.class).in(Scopes.SINGLETON);
    bind(SingularityCooldown.class).in(Scopes.SINGLETON);
    bind(SingularityDeployHealthHelper.class).in(Scopes.SINGLETON);
    bind(SingularityCooldown.class).in(Scopes.SINGLETON);
    bind(SingularityHealthchecker.class).in(Scopes.SINGLETON);
    bind(SingularityNewTaskChecker.class).in(Scopes.SINGLETON);
    bind(SingularityTaskReconciliation.class).in(Scopes.SINGLETON);
    bind(SingularityMailPoller.class).in(Scopes.SINGLETON);
    bind(SingularityDisasterDetectionPoller.class).in(Scopes.SINGLETON);
    bind(SingularityPriorityKillPoller.class).in(Scopes.SINGLETON);

    bind(SingularitySchedulerStateCache.class);
  }

  @Provides
  @Singleton
  public OfferCache getOfferCache(SingularityConfiguration configuration, Injector injector) {
    if (!configuration.isCacheOffers()) {
      return injector.getInstance(SingularityNoOfferCache.class);
    }

    return injector.getInstance(SingularityOfferCache.class);
  }

}
