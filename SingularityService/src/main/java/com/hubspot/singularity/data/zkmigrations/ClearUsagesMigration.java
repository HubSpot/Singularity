package com.hubspot.singularity.data.zkmigrations;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException.NoNodeException;

@Singleton
public class ClearUsagesMigration extends ZkDataMigration {

  private final CuratorFramework curator;

  @Inject
  public ClearUsagesMigration(CuratorFramework curator) {
    super(15);
    this.curator = curator;
  }

  @Override
  public void applyMigration() {
    try {
      try {
        // Data format has changed and usage will repopulate when the poller runs
        curator.delete().deletingChildrenIfNeeded().forPath("/usage/slaves");
        curator.delete().deletingChildrenIfNeeded().forPath("/usage/tasks");
      } catch (NoNodeException nee) {
        // don't care
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
