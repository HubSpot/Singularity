package com.hubspot.singularity.scheduler;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.hubspot.singularity.data.history.SingularityHistoryPurger;
import com.hubspot.singularity.helpers.RebalancingHelper;
import com.hubspot.singularity.mesos.SingularityMesosOfferScheduler;
import com.hubspot.singularity.mesos.SingularityMesosTaskPrioritizer;

public class SingularitySchedulerModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(SingularityHealthchecker.class).in(Scopes.SINGLETON);
    bind(SingularityNewTaskChecker.class).in(Scopes.SINGLETON);
    bind(SingularityCleanupPoller.class).in(Scopes.SINGLETON);
    bind(SingularityExpiringUserActionPoller.class).in(Scopes.SINGLETON);
    bind(SingularityHistoryPurger.class).in(Scopes.SINGLETON);
    bind(SingularityAgentReconciliationPoller.class).in(Scopes.SINGLETON);
    bind(SingularityCrashLoopPoller.class).in(Scopes.SINGLETON);
    bind(SingularityDeployPoller.class).in(Scopes.SINGLETON);
    bind(SingularitySchedulerPoller.class).in(Scopes.SINGLETON);
    bind(SingularityJobPoller.class).in(Scopes.SINGLETON);
    bind(SingularityTaskShellCommandDispatchPoller.class).in(Scopes.SINGLETON);
    bind(SingularityTaskReconciliationPoller.class).in(Scopes.SINGLETON);
    bind(SingularityScheduler.class).in(Scopes.SINGLETON);
    bind(SingularityCrashLoopChecker.class).in(Scopes.SINGLETON);
    bind(SingularityDeployChecker.class).in(Scopes.SINGLETON);
    bind(SingularityCleaner.class).in(Scopes.SINGLETON);
    bind(SingularityCrashLoops.class).in(Scopes.SINGLETON);
    bind(SingularityDeployHealthHelper.class).in(Scopes.SINGLETON);
    bind(SingularityCrashLoops.class).in(Scopes.SINGLETON);
    bind(SingularityNewTaskChecker.class).in(Scopes.SINGLETON);
    bind(SingularityTaskReconciliation.class).in(Scopes.SINGLETON);
    bind(SingularityMailPoller.class).in(Scopes.SINGLETON);
    bind(SingularityDisasterDetectionPoller.class).in(Scopes.SINGLETON);
    bind(SingularityPriorityKillPoller.class).in(Scopes.SINGLETON);
    bind(SingularityUsageCleanerPoller.class).in(Scopes.SINGLETON);
    bind(SingularityUsagePoller.class).in(Scopes.SINGLETON);
    bind(SingularityTaskShuffler.class).in(Scopes.SINGLETON);
    bind(SingularityMesosTaskPrioritizer.class).in(Scopes.SINGLETON);
    bind(SingularityMesosOfferScheduler.class).in(Scopes.SINGLETON);
    bind(SingularityLeaderCache.class).in(Scopes.SINGLETON);
    bind(SingularityLeaderCacheCoordinator.class).in(Scopes.SINGLETON);
    bind(SingularityAutoScaleSpreadAllPoller.class).in(Scopes.SINGLETON);
    bind(SingularityMesosHeartbeatChecker.class).in(Scopes.SINGLETON);
    bind(RebalancingHelper.class).in(Scopes.SINGLETON);
    bind(SingularityUpstreamPoller.class).in(Scopes.SINGLETON);
    bind(SingularityUpstreamChecker.class).in(Scopes.SINGLETON);
  }
}
