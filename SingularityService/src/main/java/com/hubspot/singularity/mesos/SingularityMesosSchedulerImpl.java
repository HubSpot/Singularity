package com.hubspot.singularity.mesos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.apache.mesos.v1.Protos;
import org.apache.mesos.v1.Protos.AgentID;
import org.apache.mesos.v1.Protos.ExecutorID;
import org.apache.mesos.v1.Protos.InverseOffer;
import org.apache.mesos.v1.Protos.MasterInfo;
import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.OfferID;
import org.apache.mesos.v1.Protos.TaskID;
import org.apache.mesos.v1.Protos.TaskStatus;
import org.apache.mesos.v1.scheduler.Protos.Event.Message;
import org.apache.mesos.v1.scheduler.Protos.Event.Subscribed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.singularity.RequestCleanupType;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityAbort.AbortReason;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityKilledTaskIdRecord;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskDestroyFrameworkMessage;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DisasterManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.transcoders.Transcoder;
import com.hubspot.singularity.mesos.SingularitySlaveAndRackManager.CheckResult;
import com.hubspot.singularity.scheduler.SingularityLeaderCacheCoordinator;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

@Singleton
public class SingularityMesosSchedulerImpl extends SingularityMesosScheduler {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMesosScheduler.class);

  private final SingularityExceptionNotifier exceptionNotifier;

  private final SingularityStartup startup;
  private final SingularityAbort abort;
  private final SingularityLeaderCacheCoordinator leaderCacheCoordinator;
  private final SingularityMesosFrameworkMessageHandler messageHandler;
  private final SingularitySlaveAndRackManager slaveAndRackManager;
  private final DisasterManager disasterManager;
  private final OfferCache offerCache;
  private final SingularityMesosOfferScheduler offerScheduler;
  private final SingularityMesosStatusUpdateHandler statusUpdateHandler;
  private final SingularityMesosSchedulerClient mesosSchedulerClient;
  private final boolean offerCacheEnabled;
  private final boolean delayWhenStatusUpdateDeltaTooLarge;
  private final long delayWhenDeltaOverMs;
  private final AtomicLong statusUpdateDeltaAvg;
  private final SingularityConfiguration configuration;
  private final TaskManager taskManager;
  private final Transcoder<SingularityTaskDestroyFrameworkMessage> transcoder;

  private final Lock stateLock;
  private final SingularitySchedulerLock lock;

  private volatile SchedulerState state;
  private Optional<Long> lastOfferTimestamp = Optional.absent();

  private final AtomicReference<MasterInfo> masterInfo = new AtomicReference<>();
  private final List<TaskStatus> queuedUpdates;

  @Inject
  SingularityMesosSchedulerImpl(SingularitySchedulerLock lock,
                            SingularityExceptionNotifier exceptionNotifier,
                            SingularityStartup startup,
                            SingularityLeaderCacheCoordinator leaderCacheCoordinator,
                            SingularityAbort abort,
                            SingularityMesosFrameworkMessageHandler messageHandler,
                            SingularitySlaveAndRackManager slaveAndRackManager,
                            OfferCache offerCache,
                            SingularityMesosOfferScheduler offerScheduler,
                            SingularityMesosStatusUpdateHandler statusUpdateHandler,
                            SingularityMesosSchedulerClient mesosSchedulerClient,
                            DisasterManager disasterManager,
                            SingularityConfiguration configuration,
                            TaskManager taskManager,
                            Transcoder<SingularityTaskDestroyFrameworkMessage> transcoder,
                            @Named(SingularityMainModule.STATUS_UPDATE_DELTA_30S_AVERAGE) AtomicLong statusUpdateDeltaAvg) {
    this.exceptionNotifier = exceptionNotifier;
    this.startup = startup;
    this.abort = abort;
    this.messageHandler = messageHandler;
    this.slaveAndRackManager = slaveAndRackManager;
    this.disasterManager = disasterManager;
    this.offerCache = offerCache;
    this.offerScheduler = offerScheduler;
    this.statusUpdateHandler = statusUpdateHandler;
    this.mesosSchedulerClient = mesosSchedulerClient;
    this.offerCacheEnabled = configuration.isCacheOffers();
    this.delayWhenStatusUpdateDeltaTooLarge = configuration.isDelayOfferProcessingForLargeStatusUpdateDelta();
    this.delayWhenDeltaOverMs = configuration.getDelayPollersWhenDeltaOverMs();
    this.statusUpdateDeltaAvg = statusUpdateDeltaAvg;
    this.taskManager = taskManager;
    this.transcoder = transcoder;
    this.leaderCacheCoordinator = leaderCacheCoordinator;
    this.queuedUpdates = Lists.newArrayList();
    this.lock = lock;
    this.stateLock = new ReentrantLock();
    this.state = SchedulerState.NOT_STARTED;
    this.configuration = configuration;
  }

  @Override
  public void subscribed(Subscribed subscribed) {
    callWithLock(() -> {
      Preconditions.checkState(state == SchedulerState.NOT_STARTED, "Asked to startup - but in invalid state: %s", state.name());

      leaderCacheCoordinator.activateLeaderCache();
      MasterInfo newMasterInfo = subscribed.getMasterInfo();
      masterInfo.set(newMasterInfo);
      startup.startup(newMasterInfo);
      stateLock.lock(); // ensure we aren't adding queued updates. calls to status updates are now blocked.

      try {
        state = SchedulerState.SUBSCRIBED;
        queuedUpdates.forEach(statusUpdateHandler::processStatusUpdate);
      } finally {
        stateLock.unlock();
      }
    }, "subscribed", false);
  }

  @Timed
  @Override
  public void resourceOffers(List<Offer> offers) {
    if (!isRunning()) {
      LOG.info("Scheduler is in state {}, declining {} offer(s)", state.name(), offers.size());
      mesosSchedulerClient.decline(offers.stream().map(Offer::getId).collect(Collectors.toList()));
      return;
    }
    callWithLock(() -> {
      final long start = System.currentTimeMillis();
      LOG.info("Received {} offer(s)", offers.size());
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
        mesosSchedulerClient.decline(offers.stream().map(Offer::getId).collect(Collectors.toList()));
        return;
      }

      if (offerCacheEnabled) {
        if (disasterManager.isDisabled(SingularityAction.CACHE_OFFERS)) {
          offerCache.disableOfferCache();
        } else {
          offerCache.enableOfferCache();
        }
      }

      List<Offer> offersToCheck = new ArrayList<>(offers);

      for (Offer offer : offers) {
        String rolesInfo = MesosUtils.getRoles(offer).toString();
        LOG.debug("Received offer ID {} with roles {} from {} ({}) for {} cpu(s), {} memory, {} ports, and {} disk", offer.getId().getValue(), rolesInfo, offer.getHostname(), offer.getAgentId().getValue(), MesosUtils.getNumCpus(offer), MesosUtils.getMemory(offer),
            MesosUtils.getNumPorts(offer), MesosUtils.getDisk(offer));

        CheckResult checkResult = slaveAndRackManager.checkOffer(offer);
        if (checkResult == CheckResult.NOT_ACCEPTING_TASKS) {
          mesosSchedulerClient.decline(Collections.singletonList(offer.getId()));
          offersToCheck.remove(offer);
          LOG.debug("Will decline offer {}, slave {} is not currently in a state to launch tasks", offer.getId().getValue(), offer.getHostname());
        }
      }

      final Set<OfferID> acceptedOffers = Sets.newHashSetWithExpectedSize(offersToCheck.size());

      try {
        List<SingularityOfferHolder> offerHolders = offerScheduler.checkOffers(offers);

        for (SingularityOfferHolder offerHolder : offerHolders) {
          if (!offerHolder.getAcceptedTasks().isEmpty()) {
            List<Offer> leftoverOffers = offerHolder.launchTasksAndGetUnusedOffers(mesosSchedulerClient);

            leftoverOffers.forEach((o) -> {
              offerCache.cacheOffer(start, o);
            });

            List<Offer> offersAcceptedFromSlave = offerHolder.getOffers();
            offersAcceptedFromSlave.removeAll(leftoverOffers);
            acceptedOffers.addAll(offersAcceptedFromSlave.stream().map(Offer::getId).collect(Collectors.toList()));
          } else {
            offerHolder.getOffers().forEach((o) -> offerCache.cacheOffer(start, o));
          }
        }
      } catch (Throwable t) {
        LOG.error("Received fatal error while handling offers - will decline all available offers", t);

        mesosSchedulerClient.decline(offersToCheck.stream()
        .filter((o) -> !acceptedOffers.contains(o.getId()))
        .map(Offer::getId)
        .collect(Collectors.toList()));

        throw t;
      }

      LOG.info("Finished handling {} new offer(s) ({}), {} accepted, {} declined/cached", offers.size(), JavaUtils.duration(start), acceptedOffers.size(),
          offers.size() - acceptedOffers.size());
    }, "offers");
  }

  @Override
  public void inverseOffers(List<InverseOffer> offers) {
    LOG.debug("Singularity is currently not able to handle inverse offers events");
  }

  @Override
  public void rescind(OfferID offerId) {
    callWithLock(() -> offerCache.rescindOffer(offerId), "rescind");
  }

  @Override
  public void rescindInverseOffer(OfferID offerId) {
    LOG.debug("Singularity is currently not able to handle inverse offers events");
  }

  @Override
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

  @Override
  public void message(Message message) {
    callWithLock(() -> {
      ExecutorID executorID = message.getExecutorId();
      AgentID slaveId = message.getAgentId();
      byte[] data = message.getData().toByteArray();
      LOG.info("Framework message from executor {} on slave {} with {} bytes of data", executorID, slaveId, data.length);
      messageHandler.handleMessage(executorID, slaveId, data);
    }, "frameworkMessage");
  }

  @Override
  public void failure(org.apache.mesos.v1.scheduler.Protos.Event.Failure failure) {
    if (failure.hasExecutorId()) {
      LOG.warn("Lost an executor {} on slave {} with status {}", failure.getExecutorId(), failure.getAgentId(), failure.getStatus());
    } else {
      callWithLock(() -> slaveLost(failure.getAgentId()), "slaveLost");
    }
  }

  @Override
  public void error(String message) {
    callWithLock(() -> {
      LOG.error("Aborting due to error: {}", message);
      abort.abort(AbortReason.MESOS_ERROR, Optional.absent());
    }, "error");
  }

  @Override
  public void heartbeat() {
    LOG.debug("Heartbeat from mesos");
  }

  public void start() throws Exception {
    mesosSchedulerClient.subscribe(configuration.getMesosConfiguration().getMaster(), this);
  }

  private void callWithLock(Runnable function, String name) {
    callWithLock(function, name, true);
  }

  private void callWithLock(Runnable function, String name, boolean ignoreIfNotRunning) {
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

  public void notifyStopping() {
    LOG.info("Scheduler is moving to stopped, current state: {}", state);

    state = SchedulerState.STOPPED;

    leaderCacheCoordinator.stopLeaderCache();
    mesosSchedulerClient.close();

    LOG.info("Scheduler now in state: {}", state);
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

  public Optional<MasterInfo> getMaster() {
    return Optional.fromNullable(masterInfo.get());
  }

  public void slaveLost(Protos.AgentID slaveId) {
    LOG.warn("Lost a slave {}", slaveId);
    slaveAndRackManager.slaveLost(slaveId);
  }

  public Optional<Long> getLastOfferTimestamp() {
    return lastOfferTimestamp;
  }

  public void killAndRecord(SingularityTaskId taskId, Optional<RequestCleanupType> requestCleanupType, Optional<TaskCleanupType> taskCleanupType, Optional<Long> originalTimestamp, Optional<Integer> retries, Optional<String> user) {
    Preconditions.checkState(isRunning());

    Optional<TaskCleanupType> maybeCleanupFromRequestAndTask = getTaskCleanupType(requestCleanupType, taskCleanupType);

    if (maybeCleanupFromRequestAndTask.isPresent() && (maybeCleanupFromRequestAndTask.get() == TaskCleanupType.USER_REQUESTED_DESTROY || maybeCleanupFromRequestAndTask.get() == TaskCleanupType.REQUEST_DELETING)) {
      Optional<SingularityTask> task = taskManager.getTask(taskId);
      if (task.isPresent()) {
        if (task.get().getTaskRequest().getDeploy().getCustomExecutorCmd().isPresent()) {
          byte[] messageBytes = transcoder.toBytes(new SingularityTaskDestroyFrameworkMessage(taskId, user));
          message(Message.newBuilder()
              .setAgentId(task.get().getMesosTask().getAgentId())
              .setExecutorId(task.get().getMesosTask().getExecutor().getExecutorId())
              .setData(ByteString.copyFrom(messageBytes))
              .build());
        } else {
          LOG.warn("Not using custom executor, will not send framework message to destroy task");
        }
      } else {
        String message = String.format("No task data available to build kill task framework message for task %s", taskId);
        exceptionNotifier.notify(message);
        LOG.error(message);
      }
    }
    mesosSchedulerClient.kill(TaskID.newBuilder().setValue(taskId.toString()).build());

    taskManager.saveKilledRecord(new SingularityKilledTaskIdRecord(taskId, System.currentTimeMillis(), originalTimestamp.or(System.currentTimeMillis()), requestCleanupType, taskCleanupType, retries.or(-1) + 1));
  }

  private Optional<TaskCleanupType> getTaskCleanupType(Optional<RequestCleanupType> requestCleanupType, Optional<TaskCleanupType> taskCleanupType) {
    if (taskCleanupType.isPresent()) {
      return taskCleanupType;
    } else {
      if (requestCleanupType.isPresent()) {
        return requestCleanupType.get().getTaskCleanupType();
      }
      return Optional.absent();
    }
  }

  public SchedulerState getState() {
    return state;
  }
}
