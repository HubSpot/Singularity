package com.hubspot.singularity;

import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.mesos.Protos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.dropwizard.lifecycle.Managed;
import com.google.inject.Inject;

public class SingularityManaged implements Managed, LeaderLatchListener {
  
  private final static Logger LOG = LoggerFactory.getLogger(SingularityManaged.class);

  private final LeaderLatch leaderLatch;
  private final SingularityDriverManager driverManager;
  private final SingularityAbort abort;
  
  private Protos.Status currentStatus;
  
  @Inject
  public SingularityManaged(SingularityDriverManager driverManager, LeaderLatch leaderLatch, SingularityAbort abort) {
    this.driverManager = driverManager;
    this.leaderLatch = leaderLatch;
    this.abort = abort;
    
    this.currentStatus = Protos.Status.DRIVER_NOT_STARTED;

    leaderLatch.addListener(this);
  }
  
  @Override
  public void start() throws Exception {
    LOG.info("Starting leader latch...");
    
    leaderLatch.start();
  }
  
  @Override
  public void stop() throws Exception {
    LOG.info("Graceful STOP initiating...");
  
    abort.stop();
  
    LOG.info("STOP finished");
  }
  
  @Override
  public void isLeader() {
    LOG.info("We are now the leader!");

    if (currentStatus != Protos.Status.DRIVER_RUNNING) {
      try {
        currentStatus = driverManager.start();
      } catch (Throwable t) {
        LOG.error("While starting driver", t);
        abort.abort();
      }
      
      if (currentStatus != Protos.Status.DRIVER_RUNNING) {
        abort.abort();
      }
    } else {
      LOG.warn("Driver was already running - took no action.");
    }    
  }
  
  @Override
  public void notLeader() {
    LOG.info("We are not the leader! - current status: " + currentStatus);

    if (currentStatus == Protos.Status.DRIVER_RUNNING) {
      try {
        currentStatus = driverManager.stop();
      } catch (Throwable t) {
        LOG.error("While stopping driver", t);
        abort.abort();
      }
    }
  }

}
