package com.hubspot.singularity.data.zkmigrations;

import javax.inject.Singleton;

import org.apache.curator.framework.CuratorFramework;

import com.google.common.base.Throwables;
import com.google.inject.Inject;

@Singleton
public class SlaveAndRackMigration extends ZkDataMigration {

  private final CuratorFramework curator;

  @Inject
  public SlaveAndRackMigration(CuratorFramework curator) {
    super(3);

    this.curator = curator;
  }

  @Override
  public void applyMigration() {
    try {
      curator.delete().deletingChildrenIfNeeded().forPath("/slaves");
      curator.delete().deletingChildrenIfNeeded().forPath("/racks");
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }


}
