package com.hubspot.singularity.data.zkmigrations;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException.NoNodeException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

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
      } catch (NoNodeException nee) {}
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


}
