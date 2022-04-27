package com.hubspot.singularity.managed;

import static com.google.common.base.Preconditions.checkState;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityLeaderController;
import com.hubspot.singularity.SingularityManagedScheduledExecutorServiceFactory;
import com.hubspot.singularity.SingularityManagedThreadPoolFactory;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.ExecutorIdGenerator;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.mesos.SingularityMesosExecutorInfoSupport;
import com.hubspot.singularity.metrics.SingularityGraphiteReporter;
import com.hubspot.singularity.scheduler.SingularityLeaderOnlyPoller;
import com.ning.http.client.AsyncHttpClient;
import io.dropwizard.lifecycle.Managed;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * All scheduler-related startup/shutdown behavior, other managed classes not represented here:
 * SingularitySmtpSender/SmtpMailer
 * SingularityExceptionNotifierManaged
 */
@Singleton
public class SingularityLifecycleManaged implements Managed {

  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityLifecycleManaged.class
  );

  private final SingularityManagedThreadPoolFactory cachedThreadPoolFactory;
  private final SingularityManagedScheduledExecutorServiceFactory scheduledExecutorServiceFactory;
  private final AsyncHttpClient asyncHttpClient;
  private final SingularityLeaderController leaderController;
  private final LeaderLatch leaderLatch;
  private final SingularityMesosExecutorInfoSupport executorInfoSupport;
  private final SingularityGraphiteReporter graphiteReporter;
  private final ExecutorIdGenerator executorIdGenerator;
  private final Set<SingularityLeaderOnlyPoller> leaderOnlyPollers;
  private final SingularityPreJettyLifecycle preJettyLifecycle;
  private final DeployManager deployManager;
  private final RequestManager requestManager;
  private final boolean readOnly;

  private final CuratorFramework curatorFramework;
  private final AtomicBoolean started = new AtomicBoolean(false);
  private final AtomicBoolean stopped = new AtomicBoolean(false);
  private final AtomicBoolean preJettyStopped = new AtomicBoolean(false);

  @Inject
  public SingularityLifecycleManaged(
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
    SingularityConfiguration configuration,
    SingularityPreJettyLifecycle preJettyLifecycle,
    RequestManager requestManager,
    DeployManager deployManager
  ) {
    this.cachedThreadPoolFactory = cachedThreadPoolFactory;
    this.scheduledExecutorServiceFactory = scheduledExecutorServiceFactory;
    this.asyncHttpClient = asyncHttpClient;
    this.curatorFramework = curatorFramework;
    this.leaderController = leaderController;
    this.leaderLatch = leaderLatch;
    this.executorInfoSupport = executorInfoSupport;
    this.graphiteReporter = graphiteReporter;
    this.executorIdGenerator = executorIdGenerator;
    this.leaderOnlyPollers = leaderOnlyPollers;
    this.readOnly = configuration.isReadOnlyInstance();
    this.preJettyLifecycle = preJettyLifecycle;
    this.requestManager = requestManager;
    this.deployManager = deployManager;
  }

  @Override
  public void start() throws Exception {
    if (!started.getAndSet(true)) {
      startCurator();
      if (!readOnly) {
        leaderLatch.start();
      } else {
        LOG.info("Registered as read only, will not attempt to become the leader");
      }
      leaderController.start(); // start the state poller
      graphiteReporter.start();
      executorIdGenerator.start();
      if (startLeaderPollers()) {
        leaderOnlyPollers.forEach(SingularityLeaderOnlyPoller::start);
      }
      preJettyLifecycle.registerShutdownHook(this::preJettyStop);
      requestManager.startApiCache(leaderLatch);
      deployManager.startApiCache(leaderLatch);
    } else {
      LOG.info("Already started, will not call again");
    }
  }

  // This will run before the application stops listening on its designated port
  private void preJettyStop() {
    if (!preJettyStopped.getAndSet(true)) {
      if (startLeaderPollers()) {
        stopNewPolls(); // Marks a boolean that will short circuit new runs of any leader only pollers
      }
      stopDirectoryFetcher(); // use http client, stop this before client
      stopStatePollerAndMesosConnection(); // Marks the scheduler as stopped
      stopHttpClients(); // Stops any additional async callbacks in healthcheck/new task check
      stopExecutors(); // Shuts down the executors for pollers and async semaphores
      stopLeaderLatch(); // let go of leadership
    } else {
      LOG.info("Already stopped pre-jetty operations");
    }
  }

  @Override
  public void stop() throws Exception {
    if (!stopped.getAndSet(true)) {
      requestManager.stopApiCache();
      deployManager.stopApiCache();
      stopOtherExecutors();
      stopCurator(); // disconnect from zk
      stopGraphiteReporter();
    } else {
      LOG.info("Already stopped");
    }
  }

  // to override in unit testing
  protected boolean startLeaderPollers() {
    return !readOnly;
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
      LOG.info("Stopping leader pollers and executors");
      cachedThreadPoolFactory.stop();
      scheduledExecutorServiceFactory.stopLeaderPollers();
    } catch (Throwable t) {
      LOG.warn("Could not stop scheduled executors ({})}", t.getMessage());
    }
  }

  // Post jetty stop, these can be hit on request paths still
  private void stopOtherExecutors() {
    try {
      LOG.info("Stopping pollers and executors");
      scheduledExecutorServiceFactory.stopOtherPollers();
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
      String thisId = leaderLatch.getId();
      if (!readOnly) {
        LOG.info("Stopping leader latch");
        leaderLatch.close();
      }
      // Wait until leader change has actually propagated
      long start = System.currentTimeMillis();
      while (
        thisId.equals(leaderLatch.getLeader().getId()) &&
        System.currentTimeMillis() - start < 15000
      ) {
        LOG.warn("Instance still has leadership, waiting and checking again");
        Thread.sleep(1000);
      }
    } catch (Throwable t) {
      LOG.warn("Could not stop leader latch ({})}", t.getMessage());
    }
  }

  private void startCurator() {
    curatorFramework.start();

    final long start = System.currentTimeMillis();

    try {
      checkState(
        curatorFramework.getZookeeperClient().blockUntilConnectedOrTimedOut(),
        "did not connect to zookeeper"
      );
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    curatorFramework.getConnectionStateListenable().addListener(leaderController);

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
}
