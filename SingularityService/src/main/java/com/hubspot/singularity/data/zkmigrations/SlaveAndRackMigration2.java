package com.hubspot.singularity.data.zkmigrations;

import javax.inject.Singleton;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException.NoNodeException;

import com.google.inject.Inject;

@Singleton
public class SlaveAndRackMigration2 extends ZkDataMigration {

  private final CuratorFramework curator;

  @Inject
  public SlaveAndRackMigration2(CuratorFramework curator) {
    super(6);

    this.curator = curator;
  }

  @Override
  public void applyMigration() {
    try {
      try {
        curator.delete().deletingChildrenIfNeeded().forPath("/slaves");
      } catch (NoNodeException nee) {}
      try {
        curator.delete().deletingChildrenIfNeeded().forPath("/racks");
      } catch (NoNodeException nee) {}
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


}
