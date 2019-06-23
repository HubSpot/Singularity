package com.hubspot.singularity.managed;

import static com.google.common.base.Preconditions.checkState;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityLeaderController;
import com.hubspot.singularity.SingularityManagedCachedThreadPoolFactory;
import com.hubspot.singularity.SingularityManagedScheduledExecutorServiceFactory;
import com.hubspot.singularity.cache.SingularityCache;
import com.hubspot.singularity.data.ExecutorIdGenerator;
import com.hubspot.singularity.mesos.SingularityMesosExecutorInfoSupport;
import com.hubspot.singularity.metrics.SingularityGraphiteReporter;
import com.hubspot.singularity.scheduler.SingularityLeaderOnlyPoller;
import com.ning.http.client.AsyncHttpClient;

import io.dropwizard.lifecycle.Managed;

/*
 * All scheduler-related startup/shutdown behavior, other managed classes not represented here:
 * SingularitySmtpSender/SmtpMailer
 * SingularityExceptionNotifierManaged
 */
@Singleton
public class SingularityLifecycleManaged implements Managed {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityLifecycleManaged.class);

  private final SingularityManagedCachedThreadPoolFactory cachedThreadPoolFactory;
  private final SingularityManagedScheduledExecutorServiceFactory scheduledExecutorServiceFactory;
  private final AsyncHttpClient asyncHttpClient;
  private final SingularityLeaderController leaderController;
  private final LeaderLatch leaderLatch;
  private final SingularityMesosExecutorInfoSupport executorInfoSupport;
  private final SingularityGraphiteReporter graphiteReporter;
  private final ExecutorIdGenerator executorIdGenerator;
  private final SingularityCache cache;
  private final Set<SingularityLeaderOnlyPoller> leaderOnlyPollers;

  private final CuratorFramework curatorFramework;
  private final AtomicBoolean started = new AtomicBoolean(false);
  private final AtomicBoolean stopped = new AtomicBoolean(false);

  @Inject
  public SingularityLifecycleManaged(SingularityManagedCachedThreadPoolFactory cachedThreadPoolFactory,
                                     SingularityManagedScheduledExecutorServiceFactory scheduledExecutorServiceFactory,
                                     AsyncHttpClient asyncHttpClient,
                                     CuratorFramework curatorFramework,
                                     SingularityLeaderController leaderController,
                                     LeaderLatch leaderLatch,
                                     SingularityMesosExecutorInfoSupport executorInfoSupport,
                                     SingularityGraphiteReporter graphiteReporter,
                                     ExecutorIdGenerator executorIdGenerator,
                                     SingularityCache cache,
                                     Set<SingularityLeaderOnlyPoller> leaderOnlyPollers) {
    this.cachedThreadPoolFactory = cachedThreadPoolFactory;
    this.scheduledExecutorServiceFactory = scheduledExecutorServiceFactory;
    this.asyncHttpClient = asyncHttpClient;
    this.curatorFramework = curatorFramework;
    this.leaderController = leaderController;
    this.leaderLatch = leaderLatch;
    this.executorInfoSupport = executorInfoSupport;
    this.graphiteReporter = graphiteReporter;
    this.executorIdGenerator = executorIdGenerator;
    this.cache = cache;
    this.leaderOnlyPollers = leaderOnlyPollers;
  }

  @Override
  public void start() throws Exception {
    if (!started.getAndSet(true)) {
      startCurator();
      leaderLatch.start();
      cache.setup();
      leaderController.start(); // start the state poller
      graphiteReporter.start();
      executorIdGenerator.start();
      leaderOnlyPollers.forEach(SingularityLeaderOnlyPoller::start);
    } else {
      LOG.info("Already started, will not call again");
    }
  }

  @Override
  public void stop() throws Exception {
    if (!stopped.getAndSet(true)) {
      stopNewPolls(); // Marks a boolean that will short circuit new runs of any leader only pollers
      stopDirectoryFetcher(); // use http client, stop this before client
      stopStatePollerAndMesosConnection(); // Marks the scheduler as stopped
      stopHttpClients(); // Stops any additional async callbacks in healthcheck/new task check
      stopExecutors(); // Shuts down the executors for pollers and async semaphores
      stopLeaderLatch(); // let go of leadership
      closeCache(); // Stop listening for distributed map updates
      stopCurator(); // disconnect from zk
      stopGraphiteReporter();
    } else {
      LOG.info("Already stopped");
    }
  }

  private void stopDirectoryFetcher() {
    try {
      LOG.info("Stopping directory fetcher");
      executorInfoSupport.stop();
    } catch (Throwable t) {
      LOG.warn("Could not stop task directory fetcher ({})}", t.getMessage());
    }
  }

  private void stopHttpClients() {
    try {
      LOG.info("Stopping http clients");
      asyncHttpClient.close();
    } catch (Throwable t) {
      LOG.warn("Could not stop http clients ({})}", t.getMessage());
    }
  }

  private void stopNewPolls() {
    LOG.info("Marking leader only pollers for shutdown");
    leaderOnlyPollers.forEach(SingularityLeaderOnlyPoller::stop);
  }

  private void stopExecutors() {
    try {
      LOG.info("Stopping pollers and executors");
      cachedThreadPoolFactory.stop();
      scheduledExecutorServiceFactory.stop();
    } catch (Throwable t) {
      LOG.warn("Could not stop scheduled executors ({})}", t.getMessage());
    }
  }

  private void stopStatePollerAndMesosConnection() {
    try {
      LOG.info("Stopping state poller");
      leaderController.stop();
    } catch (Throwable t) {
      LOG.warn("Could not stop state poller ({})}", t.getMessage());
    }
  }

  private void stopGraphiteReporter() {
    try {
      LOG.info("Stopping graphite reporter");
      graphiteReporter.stop();
    } catch (Throwable t) {
      LOG.warn("Could not stop graphite reporter ({})}", t.getMessage());
    }
  }

  private void stopLeaderLatch() {
    try {
      LOG.info("Stopping leader latch");
      leaderLatch.close();
    } catch (Throwable t) {
      LOG.warn("Could not stop leader latch ({})}", t.getMessage());
    }
  }

  private void startCurator() {
    curatorFramework.start();

    final long start = System.currentTimeMillis();

    try {
      checkState(curatorFramework.getZookeeperClient().blockUntilConnectedOrTimedOut(), "did not connect to zookeeper");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    LOG.info("Connected to ZK after {}", JavaUtils.duration(start));
  }

  private void stopCurator() {
    try {
      LOG.info("Stopping curator");
      curatorFramework.close();
    } catch (Throwable t) {
      LOG.warn("Could not close curator ({})", t.getMessage());
    }
  }

  private void closeCache() {
    try {
      LOG.info("Closing cache");
      cache.close();
    } catch (Throwable t) {
      LOG.warn("Could not close cache", t);
    }
  }
}
