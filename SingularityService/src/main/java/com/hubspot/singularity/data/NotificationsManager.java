package com.hubspot.singularity.data;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.jetbrains.annotations.NotNull;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.hubspot.singularity.config.SingularityConfiguration;

public class NotificationsManager extends CuratorManager {
  private static final String NOTIFICATIONS_ROOT = "/notifications";
  private static final String BLACKLIST_ROOT = NOTIFICATIONS_ROOT + "/blacklist";

  @Inject
  public NotificationsManager(CuratorFramework curator, SingularityConfiguration configuration, MetricRegistry metricRegistry) {
    super(curator, configuration, metricRegistry);
  }

  public void addToBlacklist(String email) {
    create(getEmailPath(email));
  }

  public void removeFromBlacklist(String email) {
    delete(getEmailPath(email));
  }

  public List<String> getBlacklist() {
    return getChildren(BLACKLIST_ROOT);
  }

  @NotNull private String getEmailPath(String email) {
    return ZKPaths.makePath(BLACKLIST_ROOT, email);
  }
}
