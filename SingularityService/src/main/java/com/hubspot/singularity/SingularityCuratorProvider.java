package com.hubspot.singularity;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.ZooKeeperConfiguration;

import io.dropwizard.lifecycle.Managed;

public class SingularityCuratorProvider implements Managed, Provider<CuratorFramework> {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityCuratorProvider.class);

  private final CuratorFramework curatorFramework;
  private final AtomicBoolean started = new AtomicBoolean();
  private final AtomicBoolean stopped = new AtomicBoolean();

  @Inject
  public SingularityCuratorProvider(final SingularityConfiguration configuration, final Set<ConnectionStateListener> connectionStateListeners) {

    checkNotNull(configuration, "configuration is null");
    checkNotNull(connectionStateListeners, "connectionStateListeners is null");

    ZooKeeperConfiguration zookeeperConfig = configuration.getZooKeeperConfiguration();

    this.curatorFramework = CuratorFrameworkFactory.builder()
        .defaultData(null)
        .sessionTimeoutMs(zookeeperConfig.getSessionTimeoutMillis())
        .connectionTimeoutMs(zookeeperConfig.getConnectTimeoutMillis())
        .connectString(zookeeperConfig.getQuorum())
        .retryPolicy(new ExponentialBackoffRetry(zookeeperConfig.getRetryBaseSleepTimeMilliseconds(), zookeeperConfig.getRetryMaxTries()))
        .namespace(zookeeperConfig.getZkNamespace()).build();

    for (ConnectionStateListener connectionStateListener : connectionStateListeners) {
      curatorFramework.getConnectionStateListenable().addListener(connectionStateListener);
    }
  }

  @Override
  public void start() {
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

  @Override
  public void stop() {
    if (started.get() && !stopped.getAndSet(true)) {
      curatorFramework.close();
    }
  }

  @Override
  public CuratorFramework get() {
    return curatorFramework;
  }
}
