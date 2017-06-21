package com.hubspot.singularity.mesos;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Singleton;

import org.apache.mesos.v1.Protos;
import org.apache.mesos.v1.Protos.MasterInfo;
import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.OfferID;
import org.apache.mesos.v1.Protos.TaskStatus;
import org.apache.mesos.v1.scheduler.Mesos;
import org.apache.mesos.v1.scheduler.Protos.Event;
import org.apache.mesos.v1.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityAbort.AbortReason;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DisasterManager;
import com.hubspot.singularity.mesos.SingularitySlaveAndRackManager.CheckResult;
import com.hubspot.singularity.scheduler.SingularityLeaderCacheCoordinator;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

@Singleton
public class SingularityMesosScheduler implements Scheduler {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMesosScheduler.class);

  private final SingularityExceptionNotifier exceptionNotifier;

  private final SingularityStartup startup;
  private final SingularityAbort abort;
  private final SingularityLeaderCacheCoordinator leaderCacheCoordinator;
  private final SingularityDriver singularityDriver;
  private final SingularityMesosFrameworkMessageHandler messageHandler;
  private final SingularitySlaveAndRackManager slaveAndRackManager;
  private final DisasterManager disasterManager;
  private final OfferCache offerCache;
  private final SingularityMesosOfferScheduler offerScheduler;
  private final SingularityMesosStatusUpdateHandler statusUpdateHandler;
  private final boolean offerCacheEnabled;
  private final boolean delayWhenStatusUpdateDeltaTooLarge;
  private final long delayWhenDeltaOverMs;
  private final AtomicLong statusUpdateDeltaAvg;

  private final Lock stateLock;

  private final SingularitySchedulerLock lock;

  private enum SchedulerState {
    STARTUP, SUBSCRIBED, STOPPED;
  }

  private volatile SchedulerState state;
  private Timer retryTimer = null;
  private Optional<Long> lastOfferTimestamp = Optional.absent();
  private final AtomicBoolean connected = new AtomicBoolean(false);
  private final AtomicReference<MasterInfo> masterInfo = new AtomicReference<>();

  private final List<TaskStatus> queuedUpdates;

  @Inject
  SingularityMesosScheduler(SingularitySchedulerLock lock,
                            SingularityExceptionNotifier exceptionNotifier,
                            SingularityStartup startup,
                            SingularityLeaderCacheCoordinator leaderCacheCoordinator,
                            SingularityAbort abort,
                            SingularityDriver singularityDriver,
                            SingularityMesosFrameworkMessageHandler messageHandler,
                            SingularitySlaveAndRackManager slaveAndRackManager,
                            OfferCache offerCache,
                            SingularityMesosOfferScheduler offerScheduler,
                            SingularityMesosStatusUpdateHandler statusUpdateHandler,
                            DisasterManager disasterManager,
                            SingularityConfiguration configuration,
                            @Named(SingularityMainModule.STATUS_UPDATE_DELTA_30S_AVERAGE) AtomicLong statusUpdateDeltaAvg) {
    this.exceptionNotifier = exceptionNotifier;

    this.startup = startup;
    this.abort = abort;
    this.singularityDriver = singularityDriver;
    this.messageHandler = messageHandler;
    this.slaveAndRackManager = slaveAndRackManager;
    this.disasterManager = disasterManager;
    this.offerCache = offerCache;
    this.offerScheduler = offerScheduler;
    this.statusUpdateHandler = statusUpdateHandler;
    this.offerCacheEnabled = configuration.isCacheOffers();
    this.delayWhenStatusUpdateDeltaTooLarge = configuration.isDelayOfferProcessingForLargeStatusUpdateDelta();
    this.delayWhenDeltaOverMs = configuration.getDelayPollersWhenDeltaOverMs();
    this.statusUpdateDeltaAvg = statusUpdateDeltaAvg;

    this.leaderCacheCoordinator = leaderCacheCoordinator;
    this.queuedUpdates = Lists.newArrayList();

    this.lock = lock;

    this.stateLock = new ReentrantLock();
    this.state = SchedulerState.STARTUP;
  }

  @Override
  public void connected(Mesos driver) {
    LOG.info("Connected to mesos");
    connected.set(true);
    callWithLock(this::registerWithRetry, "subscribe", false);
  }

  @Override
  public void disconnected(Mesos driver) {
    connected.set(true);
    // TODO - attempt to reconnect, bail if not
    LOG.warn("Scheduler/Driver disconnected");
  }

  @Override
  public void received(Mesos driver, Event event) {
    LOG.trace("Received mesos event {}", event);
    switch (event.getType()) {
      case SUBSCRIBED:
        Event.Subscribed subscribed = event.getSubscribed();
        callWithLock(() -> startup(subscribed.getMasterInfo()), "subscribed", false);
      case OFFERS:
        List<Offer> offers = ImmutableList.copyOf(event.getOffers().getOffersList());
        if (!isRunning()) {
          LOG.info("Scheduler is in state {}, declining {} offer(s)", state.name(), offers.size());
          declineOffers(offers);
          return;
        }
        callWithLock(() -> resourceOffers(offers), "resourceOffers");
        break;
      case RESCIND:
        callWithLock(() -> offerRescinded(event.getRescind().getOfferId()), "offerRescinded");
        break;
      case INVERSE_OFFERS:
      case RESCIND_INVERSE_OFFER:
        LOG.debug("Singularity is currently not able to handle inverse offers events");
        break;
      case UPDATE:
        Protos.TaskStatus status = event.getUpdate().getStatus();
        statusUpdate(status);
        break;
      case MESSAGE:
        Event.Message m = event.getMessage();
        callWithLock(() -> frameworkMessage(m.getExecutorId(), m.getAgentId(), m.getData().toByteArray()), "frameworkMessage");
        break;
      case ERROR:
        callWithLock(() -> {
          LOG.error("Aborting due to error: {}", event.getError());
          abort.abort(AbortReason.MESOS_ERROR, Optional.absent());
        }, "error");
        break;
      case FAILURE:
        Event.Failure failure = event.getFailure();
        if (failure.hasExecutorId()) {
          LOG.warn("Lost an executor {} on slave {} with status {}", failure.getExecutorId(), failure.getAgentId(), failure.getStatus());
        } else {
          callWithLock(() -> slaveLost(failure.getAgentId()), "slaveLost");
        }
        break;
      case HEARTBEAT:
        // I'm sure this is important somehow...
        break;
      default:
        LOG.warn("Unknown mesos event {}", event);
        break;
    }
  }

  public void start() {
    singularityDriver.start(this);
  }

  private void callWithLock(Runnable function, String name) {
    callWithLock(function, name, true);
  }

  private void callWithLock(Runnable function, String name, boolean ignoreIfNotRunning) {
    if (!connected.get()) {
      // This should never happen
      LOG.warn("Attempted to process message, but not connected");
    }
    if (ignoreIfNotRunning && !isRunning()) {
      LOG.info("Ignoring {} because scheduler isn't running ({})", name, state);
      return;
    }
    final long start = lock.lock(name);
    try {
      function.run();
    } catch (Throwable t) {
      LOG.error("Scheduler threw an uncaught exception - exiting", t);
      exceptionNotifier.notify(String.format("Scheduler threw an uncaught exception (%s)", t.getMessage()), t);
      abort.abort(AbortReason.UNRECOVERABLE_ERROR, Optional.of(t));
    } finally {
      lock.unlock(name, start);
    }
  }

  public synchronized void registerWithRetry() {
    if (state == SchedulerState.SUBSCRIBED || state == SchedulerState.STOPPED) {
      cancelRetryTimer();
      return;
    }

    singularityDriver.subscribe();
  }

  private void cancelRetryTimer() {
    // Cancel previously active timer (if one exists).
    if (retryTimer != null) {
      retryTimer.cancel();
      retryTimer.purge();
    }

    retryTimer = null;
  }

  public void notifyStopping() {
    LOG.info("Scheduler is moving to stopped, current state: {}", state);

    state = SchedulerState.STOPPED;

    leaderCacheCoordinator.stopLeaderCache();

    LOG.info("Scheduler now in state: {}", state);
  }

  public void startup(MasterInfo newMasterInfo) {
    Preconditions.checkState(state == SchedulerState.STARTUP, "Asked to startup - but in invalid state: %s", state.name());

    leaderCacheCoordinator.activateLeaderCache();
    masterInfo.set(newMasterInfo);
    startup.startup(newMasterInfo);
    stateLock.lock(); // ensure we aren't adding queued updates. calls to status updates are now blocked.

    try {
      state = SchedulerState.SUBSCRIBED;
      queuedUpdates.forEach(statusUpdateHandler::processStatusUpdate);
    } finally {
      stateLock.unlock();
    }
  }

  public boolean isRunning() {
    return state == SchedulerState.SUBSCRIBED;
  }

  public void setSubscribed() {
    stateLock.lock();
    try {
      state = SchedulerState.SUBSCRIBED;
    } finally {
      stateLock.unlock();
    }
  }

  public void statusUpdate(TaskStatus status) {
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
      statusUpdateHandler.processStatusUpdate(status);
    } catch (Throwable t) {
      LOG.error("Scheduler threw an uncaught exception - exiting", t);
      exceptionNotifier.notify(String.format("Scheduler threw an uncaught exception (%s)", t.getMessage()), t);
      abort.abort(AbortReason.UNRECOVERABLE_ERROR, Optional.of(t));
    } finally {
      LOG.debug("Handled status update for {} in {}", status.getTaskId().getValue(), JavaUtils.duration(start));
    }
  }

  public Optional<MasterInfo> getMaster() {
    return Optional.fromNullable(masterInfo.get());
  }

  @Timed
  public void resourceOffers(List<Offer> offers) {
    final long start = System.currentTimeMillis();
    LOG.info("Received {} offer(s)", offers.size());
    lastOfferTimestamp = Optional.of(System.currentTimeMillis());
    boolean delclineImmediately = false;
    if (disasterManager.isDisabled(SingularityAction.PROCESS_OFFERS)) {
      LOG.info("Processing offers is currently disabled, declining {} offers", offers.size());
      delclineImmediately = true;
    }
    if (delayWhenStatusUpdateDeltaTooLarge && statusUpdateDeltaAvg.get() > delayWhenDeltaOverMs) {
      LOG.info("Status update delta is too large ({}), declining offers while status updates catch up", statusUpdateDeltaAvg.get());
      delclineImmediately = true;
    }

    if (delclineImmediately) {
      for (Protos.Offer offer : offers) {
        singularityDriver.declineOffer(offer.getId());
      }
      return;
    }

    if (offerCacheEnabled) {
      if (disasterManager.isDisabled(SingularityAction.CACHE_OFFERS)) {
        offerCache.disableOfferCache();
      } else {
        offerCache.enableOfferCache();
      }
    }

    List<Protos.Offer> offersToCheck = new ArrayList<>(offers);

    for (Offer offer : offers) {
      String rolesInfo = MesosUtils.getRoles(offer).toString();
      LOG.debug("Received offer ID {} with roles {} from {} ({}) for {} cpu(s), {} memory, {} ports, and {} disk", offer.getId().getValue(), rolesInfo, offer.getHostname(), offer.getAgentId().getValue(), MesosUtils.getNumCpus(offer), MesosUtils.getMemory(offer),
          MesosUtils.getNumPorts(offer), MesosUtils.getDisk(offer));

      CheckResult checkResult = slaveAndRackManager.checkOffer(offer);
      if (checkResult == CheckResult.NOT_ACCEPTING_TASKS) {
        singularityDriver.declineOffer(offer.getId());
        offersToCheck.remove(offer);
        LOG.debug("Will decline offer {}, slave {} is not currently in a state to launch tasks", offer.getId().getValue(), offer.getHostname());
      }
    }

    final Set<OfferID> acceptedOffers = Sets.newHashSetWithExpectedSize(offersToCheck.size());

    try {
      List<SingularityOfferHolder> offerHolders = offerScheduler.checkOffers(offers);

      for (SingularityOfferHolder offerHolder : offerHolders) {
        if (!offerHolder.getAcceptedTasks().isEmpty()) {
          offerHolder.launchTasks(singularityDriver);

          acceptedOffers.add(offerHolder.getOffer().getId());
        } else {
          offerCache.cacheOffer(start, offerHolder.getOffer());
        }
      }
    } catch (Throwable t) {
      LOG.error("Received fatal error while handling offers - will decline all available offers", t);

      for (Protos.Offer offer : offersToCheck) {
        if (acceptedOffers.contains(offer.getId())) {
          continue;
        }

        singularityDriver.declineOffer(offer.getId());
      }

      throw t;
    }

    LOG.info("Finished handling {} new offer(s) ({}), {} accepted, {} declined/cached", offers.size(), JavaUtils.duration(start), acceptedOffers.size(),
        offers.size() - acceptedOffers.size());
  }

  public void offerRescinded(Protos.OfferID offerId) {
    LOG.info("Offer {} rescinded", offerId);
    offerCache.rescindOffer(offerId);
  }

  void frameworkMessage(Protos.ExecutorID executorId, Protos.AgentID slaveId, byte[] data) {
    LOG.info("Framework message from executor {} on slave {} with {} bytes of data", executorId, slaveId, data.length);

    messageHandler.handleMessage(executorId, slaveId, data);
  }

  public void slaveLost(Protos.AgentID slaveId) {
    LOG.warn("Lost a slave {}", slaveId);

    slaveAndRackManager.slaveLost(slaveId);
  }

  public boolean isConnected() {
    return connected.get();
  }

  public Optional<Long> getLastOfferTimestamp() {
    return lastOfferTimestamp;
  }

  void declineOffers(List<Offer> offers) {
    offers.forEach((o) -> singularityDriver.declineOffer(o.getId()));
  }
}
