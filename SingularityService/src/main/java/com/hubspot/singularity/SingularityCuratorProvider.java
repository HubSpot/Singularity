package com.hubspot.singularity;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.ZooKeeperConfiguration;

public class SingularityCuratorProvider implements Provider<CuratorFramework> {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityCuratorProvider.class);

  private final CuratorFramework curatorFramework;

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
  public CuratorFramework get() {
    return curatorFramework;
  }
}
