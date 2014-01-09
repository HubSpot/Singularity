package com.hubspot.singularity.mesos;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.scheduler.SingularityCleanupPoller;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.*;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SingularityMesosSchedulerDelegator implements Scheduler {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityMesosSchedulerDelegator.class);

  private final SingularityMesosScheduler scheduler;
  private final SingularityStartup startup;
  private final SingularityAbort abort;
  
  private final Lock stateLock;
  private final Lock lock;

  private enum SchedulerState {
    STARTUP, RUNNING, STOPPED;
  }

  private volatile SchedulerState state;
  private final List<Protos.TaskStatus> queuedUpdates;
  private final SingularityCleanupPoller cleanupPoller;
  
  private Optional<Long> lastOfferTimestamp;
  private MasterInfo master;
  
  @Inject
  public SingularityMesosSchedulerDelegator(SingularityMesosScheduler scheduler, SingularityStartup startup, SingularityAbort abort, SingularityCleanupPoller cleanupPoller) {
    this.scheduler = scheduler;
    this.startup = startup;
    this.abort = abort;
    this.cleanupPoller = cleanupPoller;
    
    this.queuedUpdates = Lists.newArrayList();

    this.lock = new ReentrantLock();
    this.stateLock = new ReentrantLock();

    this.state = SchedulerState.STARTUP;
    this.lastOfferTimestamp = Optional.absent();
  }
  
  public Optional<Long> getLastOfferTimestamp() {
    return lastOfferTimestamp;
  }
  
  public MasterInfo getMaster() {
    return master;
  }
  
  // TODO should the lock wait on a timeout and then notify that it's taking a while?
  
  public void lock() {
    lock.lock();
  }

  public void release() {
    lock.unlock();
  }
  
  public void notifyStopping() {
    LOG.info("Scheduler is moving to stopped, current state: " + state);
    
    cleanupPoller.stop();
    state = SchedulerState.STOPPED;
  
    LOG.info("Scheduler now in state: " + state);
  }
  
  private void handleUncaughtSchedulerException(Throwable t) {
    LOG.error("Scheduler threw an uncaught exception - exiting", t);

    abort.abort();
  }

  private void startup(SchedulerDriver driver, MasterInfo masterInfo) {
    Preconditions.checkState(state == SchedulerState.STARTUP, "Asked to startup - but in invalid state: %s", state.name());
    
    master = masterInfo;
    
    startup.startup(masterInfo);

    cleanupPoller.start(this);
    
    stateLock.lock(); // ensure we aren't adding queued updates. calls to status updates are now blocked.

    try {
      state = SchedulerState.RUNNING; // calls to resource offers will now block, since we are already scheduler locked.

      for (Protos.TaskStatus status : queuedUpdates) {
        scheduler.statusUpdate(driver, status);
      }
      
    } finally {
      stateLock.unlock();
    }
  }

  @Override
  public void registered(SchedulerDriver driver, FrameworkID frameworkId, MasterInfo masterInfo) {
    lock();

    try {
      startup(driver, masterInfo);

      scheduler.registered(driver, frameworkId, masterInfo);
    } catch (Throwable t) {
      handleUncaughtSchedulerException(t);
    } finally {
      release();
    }
  }

  @Override
  public void reregistered(SchedulerDriver driver, MasterInfo masterInfo) {
    lock();

    try {
      startup(driver, masterInfo);

      scheduler.reregistered(driver, masterInfo);
    } catch (Throwable t) {
      handleUncaughtSchedulerException(t);
    } finally {
      release();
    }
  }

  private boolean isRunning() {
    return state == SchedulerState.RUNNING;
  }
  
  @Override
  public void resourceOffers(SchedulerDriver driver, List<Offer> offers) {
    lastOfferTimestamp = Optional.of(System.currentTimeMillis());
    
    if (!isRunning()) {
      LOG.info(String.format("Scheduler is in state %s, declining %s offer(s)", state.name(), offers.size()));

      for (Protos.Offer offer : offers) {
        driver.declineOffer(offer.getId());
      }

      return;
    }

    lock();

    try {
      scheduler.resourceOffers(driver, offers);
    } catch (Throwable t) {
      handleUncaughtSchedulerException(t);
    } finally {
      release();
    }
  }

  @Override
  public void offerRescinded(SchedulerDriver driver, OfferID offerId) {
    if (!isRunning()) {
      return;
    }
    
    lock();

    try {
      scheduler.offerRescinded(driver, offerId);
    } catch (Throwable t) {
      handleUncaughtSchedulerException(t);
    } finally {
      release();
    }
  }

  @Override
  public void statusUpdate(SchedulerDriver driver, TaskStatus status) {
    stateLock.lock();

    try {
      if (!isRunning()) {
        LOG.info(String.format("Scheduler is in state %s, queueing an update %s - %s queued updates so far", state.name(), status, queuedUpdates.size()));

        queuedUpdates.add(status);

        return;
      }
    } finally {
      stateLock.unlock();
    }

    lock();
    
    try {
      scheduler.statusUpdate(driver, status);
    } catch (Throwable t) {
      handleUncaughtSchedulerException(t);
    } finally {
      release();
    }
  }

  @Override
  public void frameworkMessage(SchedulerDriver driver, ExecutorID executorId, SlaveID slaveId, byte[] data) {
    if (!isRunning()) {
      return;
    }
    
    lock();

    try {
      scheduler.frameworkMessage(driver, executorId, slaveId, data);
    } catch (Throwable t) {
      handleUncaughtSchedulerException(t);
    } finally {
      release();
    }
  }

  @Override
  public void disconnected(SchedulerDriver driver) {
    if (!isRunning()) {
      return;
    }
    
    lock();
    
    try {
      scheduler.disconnected(driver);
    } catch (Throwable t) {
      handleUncaughtSchedulerException(t);
    } finally {
      release();
    }
  }

  @Override
  public void slaveLost(SchedulerDriver driver, SlaveID slaveId) {
    if (!isRunning()) {
      return;
    }
    
    lock();
    
    try {
      scheduler.slaveLost(driver, slaveId);
    } catch (Throwable t) {
      handleUncaughtSchedulerException(t);
    } finally {
      release();
    }
  }

  @Override
  public void executorLost(SchedulerDriver driver, ExecutorID executorId, SlaveID slaveId, int status) {
    if (!isRunning()) {
      return;
    }
    
    lock();
    
    try {
      scheduler.executorLost(driver, executorId, slaveId, status);
    } catch (Throwable t) {
      handleUncaughtSchedulerException(t);
    } finally {
      release();
    }
  }

  @Override
  public void error(SchedulerDriver driver, String message) {
    if (!isRunning()) {
      return;
    }
    
    lock();
    
    try {
      scheduler.error(driver, message);
    } catch (Throwable t) {
      handleUncaughtSchedulerException(t);
    } finally {
      release();
    }
  }

}
