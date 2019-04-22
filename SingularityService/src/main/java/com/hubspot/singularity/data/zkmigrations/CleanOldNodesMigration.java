package com.hubspot.singularity.data.zkmigrations;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

public class CleanOldNodesMigration extends ZkDataMigration {
  private final CuratorFramework curatorFramework;

  @Inject
  public CleanOldNodesMigration(CuratorFramework curatorFramework) {
    super(12);
    this.curatorFramework = curatorFramework;
  }

  @Override
  public void applyMigration() {
    List<String> toClean = ImmutableList.of("/disasters/previous-stats", "/disasters/stats", "/disasters/task-credits", "/offer-state");
    try {
      for (String node : toClean) {
        if (curatorFramework.checkExists().forPath(node) != null) {
          curatorFramework.delete().deletingChildrenIfNeeded().forPath(node);
        }
      }
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }
}
