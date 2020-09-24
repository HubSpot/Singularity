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
  private static final String BLOCKLIST_PATH = ZK_ROOT_PATH + "/blacklist";

  @Inject
  public ShuffleConfigurationManager(
    CuratorFramework curator,
    SingularityConfiguration configuration,
    MetricRegistry metricRegistry
  ) {
    super(curator, configuration, metricRegistry);
  }

  private String getShuffleBlocklistPath(String requestId) {
    return ZKPaths.makePath(BLOCKLIST_PATH, requestId);
  }

  public List<String> getShuffleBlocklist() {
    return getChildren(BLOCKLIST_PATH);
  }

  public SingularityCreateResult addToShuffleBlocklist(String requestId) {
    return create(getShuffleBlocklistPath(requestId));
  }

  public SingularityDeleteResult removeFromShuffleBlocklist(String requestId) {
    return delete(getShuffleBlocklistPath(requestId));
  }

  public boolean isOnShuffleBlocklist(String requestId) {
    return exists(getShuffleBlocklistPath(requestId));
  }
}
