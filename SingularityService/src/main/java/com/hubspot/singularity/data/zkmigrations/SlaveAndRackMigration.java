package com.hubspot.singularity.data.zkmigrations;

import javax.inject.Singleton;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException.NoNodeException;
import com.google.common.base.Throwables;
import com.google.inject.Inject;

@Singleton
public class SlaveAndRackMigration extends AbstractZkDataMigration {
    @Inject
    public SlaveAndRackMigration(CuratorFramework curator) {
        super(3);
        this.curator = curator;
    }
}
