package com.hubspot.singularity.managed;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityLeaderController;
import com.hubspot.singularity.SingularityManagedScheduledExecutorServiceFactory;
import com.hubspot.singularity.SingularityManagedThreadPoolFactory;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.ExecutorIdGenerator;
import com.hubspot.singularity.mesos.SingularityMesosExecutorInfoSupport;
import com.hubspot.singularity.metrics.SingularityGraphiteReporter;
import com.hubspot.singularity.scheduler.SingularityLeaderOnlyPoller;
import com.ning.http.client.AsyncHttpClient;
import java.util.Set;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;

public class SingularityLifecycleManagedTest extends SingularityLifecycleManaged {

  @Inject
  public SingularityLifecycleManagedTest(
    SingularityManagedThreadPoolFactory cachedThreadPoolFactory,
    SingularityManagedScheduledExecutorServiceFactory scheduledExecutorServiceFactory,
    AsyncHttpClient asyncHttpClient,
    CuratorFramework curatorFramework,
    SingularityLeaderController leaderController,
    LeaderLatch leaderLatch,
    SingularityMesosExecutorInfoSupport executorInfoSupport,
    SingularityGraphiteReporter graphiteReporter,
    ExecutorIdGenerator executorIdGenerator,
    Set<SingularityLeaderOnlyPoller> leaderOnlyPollers,
    SingularityConfiguration configuration
  ) {
    super(
      cachedThreadPoolFactory,
      scheduledExecutorServiceFactory,
      asyncHttpClient,
      curatorFramework,
      leaderController,
      leaderLatch,
      executorInfoSupport,
      graphiteReporter,
      executorIdGenerator,
      leaderOnlyPollers,
      configuration
    );
  }

  @Override
  protected boolean startLeaderPollers() {
    return false;
  }
}
