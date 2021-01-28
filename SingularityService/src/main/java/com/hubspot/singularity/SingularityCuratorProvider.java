package com.hubspot.singularity;

import static com.google.common.base.Preconditions.checkNotNull;

import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.ZooKeeperConfiguration;
import com.hubspot.singularity.data.LoggingCuratorFramework;
import javax.inject.Inject;
import javax.inject.Provider;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.SessionConnectionStateErrorPolicy;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingularityCuratorProvider implements Provider<CuratorFramework> {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityCuratorProvider.class
  );

  private final CuratorFramework curatorFramework;

  @Inject
  public SingularityCuratorProvider(final SingularityConfiguration configuration) {
    checkNotNull(configuration, "configuration is null");

    ZooKeeperConfiguration zookeeperConfig = configuration.getZooKeeperConfiguration();

    CuratorFramework tempCuratorFramework = CuratorFrameworkFactory
      .builder()
      .defaultData(null)
      .connectionStateErrorPolicy(new SessionConnectionStateErrorPolicy())
      .sessionTimeoutMs(zookeeperConfig.getSessionTimeoutMillis())
      .connectionTimeoutMs(zookeeperConfig.getConnectTimeoutMillis())
      .connectString(zookeeperConfig.getQuorum())
      .retryPolicy(
        new ExponentialBackoffRetry(
          zookeeperConfig.getRetryBaseSleepTimeMilliseconds(),
          zookeeperConfig.getRetryMaxTries()
        )
      )
      .namespace(zookeeperConfig.getZkNamespace())
      .build();

    if (configuration.useLoggingCuratorFramework()) {
      tempCuratorFramework = new LoggingCuratorFramework(tempCuratorFramework);
    }

    this.curatorFramework = tempCuratorFramework;
  }

  @Override
  public CuratorFramework get() {
    return curatorFramework;
  }
}
