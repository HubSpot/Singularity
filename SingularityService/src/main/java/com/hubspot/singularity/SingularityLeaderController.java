package com.hubspot.singularity;

import io.dropwizard.lifecycle.Managed;

import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.MasterInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;

public class SingularityLeaderController implements Managed, LeaderLatchListener {
  
  private final static Logger LOG = LoggerFactory.getLogger(SingularityLeaderController.class);

  private final LeaderLatch leaderLatch;
  private final SingularityDriverManager driverManager;
  private final SingularityAbort abort;
  private final SingularityStatePoller statePoller;
  
  private boolean isMaster;
  private Protos.Status currentStatus;
  
  @Inject
  public SingularityLeaderController(SingularityDriverManager driverManager, LeaderLatch leaderLatch, SingularityAbort abort, SingularityStatePoller statePoller) {
    this.driverManager = driverManager;
    this.leaderLatch = leaderLatch;
    this.abort = abort;
    this.statePoller = statePoller;
    
    this.currentStatus = Protos.Status.DRIVER_NOT_STARTED;
    this.isMaster = false;
    
    leaderLatch.addListener(this);
  }
  
  @Override
  public void start() throws Exception {
    LOG.info("Starting leader latch...");
    
    statePoller.start(this, abort);

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
    
    isMaster = true;
    
    if (currentStatus != Protos.Status.DRIVER_RUNNING) {
      try {
        currentStatus = driverManager.start();
        statePoller.updateStateNow();
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
  
  public boolean isMaster() {
    return isMaster;
  }

  public Optional<MasterInfo> getMaster() {
    return driverManager.getMaster();
  }
  
  public Optional<Long> getLastOfferTimestamp() {
    return driverManager.getLastOfferTimestamp();
  }
  
  public Protos.Status getCurrentStatus() {
    return currentStatus;
  }

  @Override
  public void notLeader() {
    LOG.info("We are not the leader! - current status: " + currentStatus);

    isMaster = false;
    
    if (currentStatus == Protos.Status.DRIVER_RUNNING) {
      try {
        currentStatus = driverManager.stop();

        statePoller.updateStateNow();
      } catch (Throwable t) {
        LOG.error("While stopping driver", t);
        abort.abort();
      }
    }
  }

}
