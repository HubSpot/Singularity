package com.hubspot.singularity;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Lists;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.ZooKeeperConfiguration;
import com.hubspot.singularity.data.LoggingCuratorFramework;
import com.hubspot.singularity.data.curator.SingularityReadOnlyCuratorFramework;
import com.hubspot.singularity.data.curator.ZkClientsLoadDistributor;
import java.util.List;
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
  public SingularityCuratorProvider(
    final SingularityConfiguration configuration,
    final SingularityManagedScheduledExecutorServiceFactory executorServiceFactory
  ) {
    checkNotNull(configuration, "configuration is null");
    if (configuration.useLoggingCuratorFramework()) {
      LOG.trace("Creating a logging curator framework");
      this.curatorFramework =
        new LoggingCuratorFramework(
          getCuratorFrameworkForSingularityInstanceType(configuration),
          executorServiceFactory
        );
    } else {
      this.curatorFramework =
        getCuratorFrameworkForSingularityInstanceType(configuration);
    }
  }

  private CuratorFramework getCuratorFrameworkForSingularityInstanceType(
    SingularityConfiguration configuration
  ) {
    ZooKeeperConfiguration zookeeperConfig = configuration.getZooKeeperConfiguration();

    if (configuration.isReadOnlyInstance()) {
      int numberOfCuratorFrameworks = Math.max(
        1,
        zookeeperConfig.getCuratorFrameworkInstances()
      );
      LOG.info(
        "Creating {} logging curator frameworks for read-only instance",
        numberOfCuratorFrameworks
      );

      List<CuratorFramework> curatorFrameworks = Lists.newArrayListWithExpectedSize(
        numberOfCuratorFrameworks
      );
      for (int i = 0; i < numberOfCuratorFrameworks; i++) {
        curatorFrameworks.add(buildCuratorFrameworkInstance(zookeeperConfig));
      }
      return new SingularityReadOnlyCuratorFramework(
        new ZkClientsLoadDistributor(curatorFrameworks)
      );
    } else {
      LOG.info("Creating curator framework for leader instance");
      return buildCuratorFrameworkInstance(zookeeperConfig);
    }
  }

  private CuratorFramework buildCuratorFrameworkInstance(
    ZooKeeperConfiguration zookeeperConfig
  ) {
    return CuratorFrameworkFactory
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
  }

  @Override
  public CuratorFramework get() {
    return curatorFramework;
  }
}
