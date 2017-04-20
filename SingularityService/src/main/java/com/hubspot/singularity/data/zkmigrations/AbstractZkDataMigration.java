package com.hubspot.singularity.data.zkmigrations;

import org.apache.curator.framework.CuratorFramework;
import com.google.common.base.Throwables;
import org.apache.zookeeper.KeeperException.NoNodeException;

public abstract class AbstractZkDataMigration extends ZkDataMigration {
    protected CuratorFramework curator;

    public AbstractZkDataMigration(int migrationNumber) {
        super(migrationNumber);
    }

    @Override
    public void applyMigration() {
        try {
            try {
                curator.delete().deletingChildrenIfNeeded().forPath("/slaves");
            } catch (NoNodeException nee) {
            }
            try {
                curator.delete().deletingChildrenIfNeeded().forPath("/racks");
            } catch (NoNodeException nee) {
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
