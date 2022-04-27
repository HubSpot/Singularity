package com.hubspot.singularity.data.zkmigrations;

import com.google.inject.Inject;
import javax.inject.Singleton;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException.NoNodeException;

@Singleton
public class AgentAndRackMigration2 extends ZkDataMigration {

  private final CuratorFramework curator;

  @Inject
  public AgentAndRackMigration2(CuratorFramework curator) {
    super(6);
    this.curator = curator;
  }

  @Override
  public void applyMigration() {
    try {
      try {
        curator.delete().deletingChildrenIfNeeded().forPath("/slaves");
      } catch (NoNodeException nee) {
        // don't care
      }
      try {
        curator.delete().deletingChildrenIfNeeded().forPath("/racks");
      } catch (NoNodeException nee) {
        // don't care
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
