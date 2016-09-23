package com.hubspot.singularity.data.zkmigrations;

import javax.inject.Singleton;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException.NoNodeException;
import com.google.common.base.Throwables;
import com.google.inject.Inject;

@Singleton
public class SlaveAndRackMigration2 extends AbstractZkDataMigration {
  @Inject
  public SlaveAndRackMigration2(CuratorFramework curator) {
    super(6);
    this.curator = curator;
  }
}
