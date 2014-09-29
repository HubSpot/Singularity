package com.hubspot.singularity;

import io.dropwizard.lifecycle.Managed;

import java.util.Optional;

import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.MasterInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

public class SingularityLeaderController implements Managed, LeaderLatchListener {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityLeaderController.class);

  private final LeaderLatch leaderLatch;
  private final SingularityDriverManager driverManager;
  private final SingularityAbort abort;
  private final SingularityStatePoller statePoller;
  private final SingularityExceptionNotifier exceptionNotifier;

  private boolean isMaster;

  @Inject
  public SingularityLeaderController(SingularityDriverManager driverManager, LeaderLatch leaderLatch, SingularityAbort abort, SingularityStatePoller statePoller, SingularityExceptionNotifier exceptionNotifier) {
    this.driverManager = driverManager;
    this.leaderLatch = leaderLatch;
    this.abort = abort;
    this.statePoller = statePoller;
    this.exceptionNotifier = exceptionNotifier;

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
    LOG.info("We are now the leader! Current status {}", driverManager.getCurrentStatus());

    isMaster = true;

    if (driverManager.getCurrentStatus() != Protos.Status.DRIVER_RUNNING) {
      try {
        driverManager.start();
        statePoller.updateStateNow();
      } catch (Throwable t) {
        LOG.error("While starting driver", t);
        exceptionNotifier.notify(t);
        abort.abort();
      }

      if (driverManager.getCurrentStatus() != Protos.Status.DRIVER_RUNNING) {
        abort.abort();
      }
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
    return driverManager.getCurrentStatus();
  }

  @Override
  public void notLeader() {
    LOG.info("We are not the leader! Current status {}", driverManager.getCurrentStatus());

    isMaster = false;

    if (driverManager.getCurrentStatus() == Protos.Status.DRIVER_RUNNING) {
      try {
        driverManager.stop();

        statePoller.updateStateNow();
      } catch (Throwable t) {
        LOG.error("While stopping driver", t);
        exceptionNotifier.notify(t);
      } finally {
        abort.abort();
      }
    }
  }

}
