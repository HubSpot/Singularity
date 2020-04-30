package com.hubspot.singularity.data;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.config.SingularityConfiguration;
import java.util.List;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShuffleConfigurationManager extends CuratorAsyncManager {
  private static final Logger LOG = LoggerFactory.getLogger(RequestManager.class);

  private static final String ZK_ROOT_PATH = "/shuffle";
  private static final String BLACKLIST_PATH = ZK_ROOT_PATH + "/blacklist";

  @Inject
  public ShuffleConfigurationManager(
    CuratorFramework curator,
    SingularityConfiguration configuration,
    MetricRegistry metricRegistry
  ) {
    super(curator, configuration, metricRegistry);
  }

  private String getShuffleBlacklistPath(String requestId) {
    return ZKPaths.makePath(BLACKLIST_PATH, requestId);
  }

  public List<String> getShuffleBlacklist() {
    return getChildren(BLACKLIST_PATH);
  }

  public SingularityCreateResult addToShuffleBlacklist(String requestId) {
    return create(getShuffleBlacklistPath(requestId));
  }

  public SingularityDeleteResult removeFromShuffleBlacklist(String requestId) {
    return delete(getShuffleBlacklistPath(requestId));
  }

  public boolean isOnShuffleBlacklist(String requestId) {
    return exists(getShuffleBlacklistPath(requestId));
  }
}
