package com.hubspot.singularity;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;

import com.codahale.dropwizard.lifecycle.Managed;
import com.google.common.io.Closeables;
import com.google.inject.Inject;
import com.hubspot.singularity.mesos.SingularityDriver;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.mesos.Protos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingularityManaged implements Managed {
  private final static Logger LOG = LoggerFactory.getLogger(SingularityManaged.class);

  private final SingularityDriver driver;
  private final CuratorFramework curator;
  private final LeaderLatch leaderLatch;

  private Protos.Status currentStatus;
  
  @Inject
  public SingularityManaged(SingularityDriver driver, CuratorFramework curator, LeaderLatch leaderLatch) {
    this.driver = driver;
    this.curator = curator;
    this.leaderLatch = leaderLatch;
    this.currentStatus = Protos.Status.DRIVER_NOT_STARTED;
  }
  
  @Override
  public void start() throws Exception {
    leaderLatch.addListener(new LeaderLatchListener() {
      @Override
      public void isLeader() {
        LOG.info("We are now the leader!");

        if (currentStatus != Protos.Status.DRIVER_RUNNING) {
          currentStatus = driver.start();
        } else {
          LOG.warn("Driver is already running?");
        }
      }

      @Override
      public void notLeader() {
        LOG.info("We are not the leader!");

        if (currentStatus == Protos.Status.DRIVER_RUNNING) {
          currentStatus = driver.stop(false);
        }
      }
    });

    leaderLatch.start();
  }
  
  @Override
  public void stop() throws Exception {
    leaderLatch.close();
    Closeables.close(curator, true);
  }

}
