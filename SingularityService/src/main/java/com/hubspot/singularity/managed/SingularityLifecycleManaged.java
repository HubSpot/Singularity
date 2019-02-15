package com.hubspot.singularity.managed;

import static com.google.common.base.Preconditions.checkState;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityAsyncHttpClient;
import com.hubspot.singularity.SingularityLeaderController;
import com.hubspot.singularity.SingularityLeaderLatch;
import com.hubspot.singularity.SingularityManagedScheduledExecutorServiceFactory;
import com.hubspot.singularity.mesos.SingularityMesosExecutorInfoSupport;
import com.hubspot.singularity.metrics.SingularityGraphiteReporter;

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
  private final SingularityAsyncHttpClient asyncHttpClient;
  private final SingularityLeaderController leaderController;
  private final SingularityLeaderLatch leaderLatch;
  private final SingularityMesosExecutorInfoSupport executorInfoSupport;
  private final SingularityGraphiteReporter graphiteReporter;

  private final CuratorFramework curatorFramework;
  private final AtomicBoolean started = new AtomicBoolean();
  private final AtomicBoolean stopped = new AtomicBoolean();

  @Inject
  public SingularityLifecycleManaged(SingularityManagedScheduledExecutorServiceFactory scheduledExecutorServiceFactory,
                                     SingularityAsyncHttpClient asyncHttpClient,
                                     CuratorFramework curatorFramework,
                                     SingularityLeaderController leaderController,
                                     SingularityLeaderLatch leaderLatch,
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
    startCurator();
    leaderLatch.start();
    leaderController.start(); // start the state poller
    graphiteReporter.start();
  }

  @Override
  public void stop() throws Exception {
    scheduledExecutorServiceFactory.stop(); //shut down pollers
    leaderController.stop(); // stop the state poller
    executorInfoSupport.stop(); // stop directory fetcher
    asyncHttpClient.close(); // Shut off most http client usages
    leaderLatch.close();
    stopCurator();
    graphiteReporter.stop();
  }

  private void startCurator() {
    if (!started.getAndSet(true)) {
      curatorFramework.start();

      final long start = System.currentTimeMillis();

      try {
        checkState(curatorFramework.getZookeeperClient().blockUntilConnectedOrTimedOut(), "did not connect to zookeeper");
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      LOG.info("Connected to ZK after {}", JavaUtils.duration(start));
    }
  }

  private void stopCurator() {
    if (started.get() && !stopped.getAndSet(true)) {
      curatorFramework.close();
    }
  }
}
