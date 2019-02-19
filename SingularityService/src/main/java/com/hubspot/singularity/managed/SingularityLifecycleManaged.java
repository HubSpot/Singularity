package com.hubspot.singularity.managed;

import static com.google.common.base.Preconditions.checkState;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityLeaderController;
import com.hubspot.singularity.SingularityManagedScheduledExecutorServiceFactory;
import com.hubspot.singularity.mesos.SingularityMesosExecutorInfoSupport;
import com.hubspot.singularity.metrics.SingularityGraphiteReporter;
import com.ning.http.client.AsyncHttpClient;

import io.dropwizard.lifecycle.Managed;

/*
 * All scheduler-related startup/shutdown behavior, other managed classes not represented here:
 * SingularitySmtpSender/SmtpMailer
 * SingularityExceptionNotifierManaged
 * ExecutorIdGenerator
 */
@Singleton
public class SingularityLifecycleManaged implements Managed {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityLifecycleManaged.class);

  private final SingularityManagedScheduledExecutorServiceFactory scheduledExecutorServiceFactory;
  private final AsyncHttpClient asyncHttpClient;
  private final SingularityLeaderController leaderController;
  private final LeaderLatch leaderLatch;
  private final SingularityMesosExecutorInfoSupport executorInfoSupport;
  private final SingularityGraphiteReporter graphiteReporter;

  private final CuratorFramework curatorFramework;
  private final AtomicBoolean started = new AtomicBoolean(false);
  private final AtomicBoolean stopped = new AtomicBoolean(false);

  @Inject
  public SingularityLifecycleManaged(SingularityManagedScheduledExecutorServiceFactory scheduledExecutorServiceFactory,
                                     AsyncHttpClient asyncHttpClient,
                                     CuratorFramework curatorFramework,
                                     SingularityLeaderController leaderController,
                                     LeaderLatch leaderLatch,
                                     SingularityMesosExecutorInfoSupport executorInfoSupport,
                                     SingularityGraphiteReporter graphiteReporter) {
    this.scheduledExecutorServiceFactory = scheduledExecutorServiceFactory;
    this.asyncHttpClient = asyncHttpClient;
    this.curatorFramework = curatorFramework;
    this.leaderController = leaderController;
    this.leaderLatch = leaderLatch;
    this.executorInfoSupport = executorInfoSupport;
    this.graphiteReporter = graphiteReporter;
  }

  @Override
  public void start() throws Exception {
    if (!started.getAndSet(true)) {
      startCurator();
      leaderLatch.start();
      leaderController.start(); // start the state poller
      graphiteReporter.start();
    } else {
      LOG.info("Already started, will not call again");
    }
  }

  @Override
  public void stop() throws Exception {
    if (!stopped.getAndSet(true)) {
      stopExecutorsAndPollers();
      stopStatePoller();
      stopDirectoryFetcher();
      stopHttpClients();
      stopLeaderLatch();
      stopCurator();
      stopGraphiteReporter();
    } else {
      LOG.info("Already stopped");
    }
  }

  private void stopDirectoryFetcher() {
    try {
      executorInfoSupport.stop();
    } catch (Throwable t) {
      LOG.warn("Could not stop task directory fetcher ({})}", t.getMessage());
    }
  }

  private void stopHttpClients() {
    try {
      asyncHttpClient.close();
    } catch (Throwable t) {
      LOG.warn("Could not stop http clients ({})}", t.getMessage());
    }
  }

  private void stopExecutorsAndPollers() {
    try {
      scheduledExecutorServiceFactory.stop();
    } catch (Throwable t) {
      LOG.warn("Could not stop scheduled executors ({})}", t.getMessage());
    }
  }

  private void stopStatePoller() {
    try {
      leaderController.stop();
    } catch (Throwable t) {
      LOG.warn("Could not stop state poller ({})}", t.getMessage());
    }
  }

  private void stopGraphiteReporter() {
    try {
      graphiteReporter.stop();
    } catch (Throwable t) {
      LOG.warn("Could not stop graphite reporter ({})}", t.getMessage());
    }
  }

  private void stopLeaderLatch() {
    try {
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
      curatorFramework.close();
    } catch (Throwable t) {
      LOG.warn("Could not close curator ({})", t.getMessage());
    }
  }
}
