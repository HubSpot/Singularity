package com.hubspot.singularity.scheduler;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.hubspot.singularity.data.history.SingularityHistoryPurger;
import com.hubspot.singularity.mesos.SingularityMesosOfferScheduler;
import com.hubspot.singularity.mesos.SingularityMesosTaskPrioritizer;

public class SingularitySchedulerModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(SingularityHealthchecker.class).in(Scopes.SINGLETON);
    bind(SingularityNewTaskChecker.class).in(Scopes.SINGLETON);
    bind(SingularityCleanupPoller.class).in(Scopes.SINGLETON);
    bind(SingularityStatusUpdateDeltaPoller.class).in(Scopes.SINGLETON);
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
    bind(SingularityUsageCleanerPoller.class).in(Scopes.SINGLETON);
    bind(SingularityUsagePoller.class).in(Scopes.SINGLETON);
    bind(SingularityMesosTaskPrioritizer.class).in(Scopes.SINGLETON);
    bind(SingularityMesosOfferScheduler.class).in(Scopes.SINGLETON);
    bind(SingularityLeaderCache.class).in(Scopes.SINGLETON);
    bind(SingularityLeaderCacheCoordinator.class).in(Scopes.SINGLETON);
    bind(SingularityAutoScaleSpreadAllPoller.class).in(Scopes.SINGLETON);
  }

}
