package com.hubspot.singularity.mesos;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.mesos.Protos.ExecutorID;
import org.apache.mesos.Protos.FrameworkID;
import org.apache.mesos.Protos.MasterInfo;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.OfferID;
import org.apache.mesos.Protos.SlaveID;
import org.apache.mesos.Protos.TaskStatus;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class SingularityMesosSchedulerDelegator implements Scheduler {

  private final SingularityMesosScheduler scheduler;
  private final Lock lock;

  private final static Logger LOG = LoggerFactory.getLogger(SingularityMesosScheduler.class);
  
  @Inject
  public SingularityMesosSchedulerDelegator(SingularityMesosScheduler scheduler) {
    this.scheduler = scheduler;
  
    this.lock = new ReentrantLock();
  }

  // TODO should the lock wait on a timeout and then notify that it's taking a while?
  
  private void lock() {
    lock.lock();
  }

  private void release() {
    lock.unlock();
  }

  // TODO should this abort?
  private void handleUncaughtSchedulerException(Throwable t) {
    LOG.error("Scheduler threw an uncaught exception", t);
  }

  @Override
  public void registered(SchedulerDriver driver, FrameworkID frameworkId, MasterInfo masterInfo) {
    lock();

    try {
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
      scheduler.reregistered(driver, masterInfo);
    } catch (Throwable t) {
      handleUncaughtSchedulerException(t);
    } finally {
      release();
    }
  }

  @Override
  public void resourceOffers(SchedulerDriver driver, List<Offer> offers) {
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
