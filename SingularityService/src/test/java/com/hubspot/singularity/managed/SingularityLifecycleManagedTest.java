package com.hubspot.singularity.managed;

import java.util.Set;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityLeaderController;
import com.hubspot.singularity.SingularityManagedCachedThreadPoolFactory;
import com.hubspot.singularity.SingularityManagedScheduledExecutorServiceFactory;
import com.hubspot.singularity.data.ExecutorIdGenerator;
import com.hubspot.singularity.mesos.SingularityMesosExecutorInfoSupport;
import com.hubspot.singularity.metrics.SingularityGraphiteReporter;
import com.hubspot.singularity.scheduler.SingularityLeaderOnlyPoller;
import com.ning.http.client.AsyncHttpClient;

public class SingularityLifecycleManagedTest extends SingularityLifecycleManaged {

  @Inject
  public SingularityLifecycleManagedTest(SingularityManagedCachedThreadPoolFactory cachedThreadPoolFactory,
                                     SingularityManagedScheduledExecutorServiceFactory scheduledExecutorServiceFactory,
                                     AsyncHttpClient asyncHttpClient,
                                     CuratorFramework curatorFramework,
                                     SingularityLeaderController leaderController,
                                     LeaderLatch leaderLatch,
                                     SingularityMesosExecutorInfoSupport executorInfoSupport,
                                     SingularityGraphiteReporter graphiteReporter,
                                     ExecutorIdGenerator executorIdGenerator,
                                     Set<SingularityLeaderOnlyPoller> leaderOnlyPollers) {
    super(cachedThreadPoolFactory, scheduledExecutorServiceFactory, asyncHttpClient, curatorFramework, leaderController, leaderLatch, executorInfoSupport, graphiteReporter, executorIdGenerator, leaderOnlyPollers);
  }

  @Override
  protected boolean startLeaderPollers() {
    return false;
  }
}
