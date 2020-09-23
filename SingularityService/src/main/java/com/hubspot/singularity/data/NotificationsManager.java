package com.hubspot.singularity.data;

import com.codahale.metrics.MetricRegistry;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.hubspot.singularity.config.SingularityConfiguration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

public class NotificationsManager extends CuratorManager {
  private static final String NOTIFICATIONS_ROOT = "/notifications";
  private static final String BLOCKLIST_ROOT = NOTIFICATIONS_ROOT + "/blacklist";

  LoadingCache<String, List<String>> cache;

  @Inject
  public NotificationsManager(
    CuratorFramework curator,
    SingularityConfiguration configuration,
    MetricRegistry metricRegistry
  ) {
    super(curator, configuration, metricRegistry);
    cache =
      CacheBuilder
        .newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build(
          new CacheLoader<String, List<String>>() {

            @Override
            public List<String> load(String key) throws Exception {
              return getChildren(key);
            }
          }
        );
  }

  public void addToBlocklist(String email) {
    create(getEmailPath(email));
    cache.invalidate(BLOCKLIST_ROOT);
  }

  public void removeFromBlocklist(String email) {
    delete(getEmailPath(email));
    cache.invalidate(BLOCKLIST_ROOT);
  }

  public List<String> getBlocklist() {
    return cache.getUnchecked(BLOCKLIST_ROOT);
  }

  private String getEmailPath(String email) {
    return ZKPaths.makePath(BLOCKLIST_ROOT, email);
  }
}
