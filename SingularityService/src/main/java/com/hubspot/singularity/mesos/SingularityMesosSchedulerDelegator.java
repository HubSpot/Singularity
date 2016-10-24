package com.hubspot.singularity.mesos;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Singleton;

import org.apache.mesos.Protos;
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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityAbort.AbortReason;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

@Singleton
public class SingularityMesosSchedulerDelegator implements Scheduler {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMesosSchedulerDelegator.class);

  private final SingularityExceptionNotifier exceptionNotifier;

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

  private Optional<Long> lastOfferTimestamp;
  private final AtomicReference<MasterInfo> masterInfoHolder = new AtomicReference<>();

  @Inject
  SingularityMesosSchedulerDelegator(@Named(SingularityMesosModule.SCHEDULER_LOCK_NAME) final Lock lock, SingularityExceptionNotifier exceptionNotifier, SingularityMesosScheduler scheduler,
      SingularityStartup startup, SingularityAbort abort) {
    this.exceptionNotifier = exceptionNotifier;

    this.scheduler = scheduler;
    this.startup = startup;
    this.abort = abort;

    this.queuedUpdates = Lists.newArrayList();

    this.lock = lock;

    this.stateLock = new ReentrantLock();
    this.state = SchedulerState.STARTUP;
    this.lastOfferTimestamp = Optional.absent();
  }

  public Optional<Long> getLastOfferTimestamp() {
    return lastOfferTimestamp;
  }

  public Optional<MasterInfo> getMaster() {
    return Optional.fromNullable(masterInfoHolder.get());
  }

  public void notifyStopping() {
    LOG.info("Scheduler is moving to stopped, current state: {}", state);

    state = SchedulerState.STOPPED;

    LOG.info("Scheduler now in state: {}", state);
  }

  private void handleUncaughtSchedulerException(Throwable t) {
    LOG.error("Scheduler threw an uncaught exception - exiting", t);

    exceptionNotifier.notify(String.format("Scheduler threw an uncaught exception (%s)", t.getMessage()), t);

    abort.abort(AbortReason.UNRECOVERABLE_ERROR, Optional.of(t));
  }

  private void startup(SchedulerDriver driver, MasterInfo masterInfo) throws Exception {
    Preconditions.checkState(state == SchedulerState.STARTUP, "Asked to startup - but in invalid state: %s", state.name());

    masterInfoHolder.set(masterInfo);

    startup.startup(masterInfo, driver);

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
    lock.lock();

    try {
      scheduler.registered(driver, frameworkId, masterInfo);

      startup(driver, masterInfo);
    } catch (Throwable t) {
      handleUncaughtSchedulerException(t);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void reregistered(SchedulerDriver driver, MasterInfo masterInfo) {
    lock.lock();

    try {
      scheduler.reregistered(driver, masterInfo);

      startup(driver, masterInfo);
    } catch (Throwable t) {
      handleUncaughtSchedulerException(t);
    } finally {
      lock.unlock();
    }
  }

  public boolean isRunning() {
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

    final long start = System.currentTimeMillis();

    lock.lock();

    try {
      scheduler.resourceOffers(driver, offers);
    } catch (Throwable t) {
      handleUncaughtSchedulerException(t);
    } finally {
      lock.unlock();

      LOG.debug("Handled {} resource offers in {}", offers.size(), JavaUtils.duration(start));
    }
  }

  @Override
  public void offerRescinded(SchedulerDriver driver, OfferID offerId) {
    if (!isRunning()) {
      LOG.info("Ignoring offer rescind message {} because scheduler isn't running ({})", offerId, state);
      return;
    }

    lock.lock();

    try {
      scheduler.offerRescinded(driver, offerId);
    } catch (Throwable t) {
      handleUncaughtSchedulerException(t);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void statusUpdate(SchedulerDriver driver, TaskStatus status) {
    final long start = System.currentTimeMillis();

    stateLock.lock();

    try {
      if (!isRunning()) {
        LOG.info("Scheduler is in state {}, queueing an update {} - {} queued updates so far", state.name(), status, queuedUpdates.size());

        queuedUpdates.add(status);

        return;
      }
    } finally {
      stateLock.unlock();
    }

    try {
      scheduler.statusUpdate(driver, status);
    } catch (Throwable t) {
      handleUncaughtSchedulerException(t);
    } finally {

      LOG.debug("Handled status update for {} in {}", status.getTaskId().getValue(), JavaUtils.duration(start));
    }
  }

  @Override
  public void frameworkMessage(SchedulerDriver driver, ExecutorID executorId, SlaveID slaveId, byte[] data) {
    if (!isRunning()) {
      LOG.info("Ignoring framework message because scheduler isn't running ({})", state);
      return;
    }

    lock.lock();

    try {
      scheduler.frameworkMessage(driver, executorId, slaveId, data);
    } catch (Throwable t) {
      handleUncaughtSchedulerException(t);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void disconnected(SchedulerDriver driver) {
    if (!isRunning()) {
      LOG.info("Ignoring disconnect because scheduler isn't running ({})", state);
      return;
    }

    lock.lock();

    try {
      scheduler.disconnected(driver);
    } catch (Throwable t) {
      handleUncaughtSchedulerException(t);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void slaveLost(SchedulerDriver driver, SlaveID slaveId) {
    if (!isRunning()) {
      LOG.info("Ignoring slave lost {} because scheduler isn't running ({})", slaveId, state);
      return;
    }

    lock.lock();

    try {
      scheduler.slaveLost(driver, slaveId);
    } catch (Throwable t) {
      handleUncaughtSchedulerException(t);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void executorLost(SchedulerDriver driver, ExecutorID executorId, SlaveID slaveId, int status) {
    if (!isRunning()) {
      LOG.info("Ignoring executor lost {} because scheduler isn't running ({})", executorId, state);
      return;
    }

    lock.lock();

    try {
      scheduler.executorLost(driver, executorId, slaveId, status);
    } catch (Throwable t) {
      handleUncaughtSchedulerException(t);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void error(SchedulerDriver driver, String message) {
    if (!isRunning()) {
      LOG.info("Ignoring error {} because scheduler isn't running ({})", message, state);
      return;
    }

    lock.lock();

    try {
      scheduler.error(driver, message);

      LOG.error("Aborting due to error: {}", message);

      abort.abort(AbortReason.MESOS_ERROR, Optional.<Throwable>absent());
    } catch (Throwable t) {
      handleUncaughtSchedulerException(t);
    } finally {
      lock.unlock();
    }
  }

}
