package com.hubspot.singularity.mesos;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.apache.mesos.v1.Protos;
import org.apache.mesos.v1.Protos.AgentID;
import org.apache.mesos.v1.Protos.DurationInfo;
import org.apache.mesos.v1.Protos.ExecutorID;
import org.apache.mesos.v1.Protos.InverseOffer;
import org.apache.mesos.v1.Protos.KillPolicy;
import org.apache.mesos.v1.Protos.MasterInfo;
import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.OfferID;
import org.apache.mesos.v1.Protos.TaskID;
import org.apache.mesos.v1.Protos.TaskStatus;
import org.apache.mesos.v1.scheduler.Protos.Event;
import org.apache.mesos.v1.scheduler.Protos.Event.Failure;
import org.apache.mesos.v1.scheduler.Protos.Event.Message;
import org.apache.mesos.v1.scheduler.Protos.Event.Subscribed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.RequestCleanupType;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityAbort.AbortReason;
import com.hubspot.singularity.SingularityKilledTaskIdRecord;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskDestroyFrameworkMessage;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DisasterManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.transcoders.Transcoder;
import com.hubspot.singularity.helpers.MesosProtosUtils;
import com.hubspot.singularity.scheduler.SingularityLeaderCacheCoordinator;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

import io.netty.handler.codec.PrematureChannelClosureException;

@Singleton
public class SingularityMesosSchedulerImpl extends SingularityMesosScheduler {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMesosScheduler.class);

  private final SingularityExceptionNotifier exceptionNotifier;

  private final SingularityStartup startup;
  private final SingularityAbort abort;
  private final SingularityLeaderCacheCoordinator leaderCacheCoordinator;
  private final SingularityMesosFrameworkMessageHandler messageHandler;
  private final SingularitySlaveAndRackManager slaveAndRackManager;
  private final OfferCache offerCache;
  private final SingularityMesosOfferScheduler offerScheduler;
  private final SingularityMesosStatusUpdateHandler statusUpdateHandler;
  private final SingularityMesosSchedulerClient mesosSchedulerClient;
  private final AtomicLong lastHeartbeatTime;
  private final SingularityConfiguration configuration;
  private final TaskManager taskManager;
  private final Transcoder<SingularityTaskDestroyFrameworkMessage> transcoder;
  private final SingularitySchedulerLock lock;

  private volatile SchedulerState state;
  private Optional<Long> lastOfferTimestamp = Optional.absent();
  private Optional<Double> heartbeatIntervalSeconds = Optional.absent();

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
                                @Named(SingularityMainModule.STATUS_UPDATE_DELTA_30S_AVERAGE) AtomicLong statusUpdateDeltaAvg,
                                @Named(SingularityMainModule.LAST_MESOS_MASTER_HEARTBEAT_TIME) AtomicLong lastHeartbeatTime) {
    this.exceptionNotifier = exceptionNotifier;
    this.startup = startup;
    this.abort = abort;
    this.messageHandler = messageHandler;
    this.slaveAndRackManager = slaveAndRackManager;
    this.offerCache = offerCache;
    this.offerScheduler = offerScheduler;
    this.statusUpdateHandler = statusUpdateHandler;
    this.mesosSchedulerClient = mesosSchedulerClient;
    this.lastHeartbeatTime = lastHeartbeatTime;
    this.taskManager = taskManager;
    this.transcoder = transcoder;
    this.leaderCacheCoordinator = leaderCacheCoordinator;
    this.queuedUpdates = Lists.newArrayList();
    this.lock = lock;
    this.state = SchedulerState.NOT_STARTED;
    this.configuration = configuration;
  }

  @Override
  public void subscribed(Subscribed subscribed) {
    callWithStateLock(() -> {
      Preconditions.checkState(state == SchedulerState.NOT_STARTED, "Asked to startup - but in invalid state: %s", state.name());

      double advertisedHeartbeatIntervalSeconds = subscribed.getHeartbeatIntervalSeconds();
      if (advertisedHeartbeatIntervalSeconds > 0) {
        heartbeatIntervalSeconds = Optional.of(advertisedHeartbeatIntervalSeconds);
      }

      // Should be called before activation of leader cache or cache could be left empty
      startup.checkMigrations();

      leaderCacheCoordinator.activateLeaderCache();
      MasterInfo newMasterInfo = subscribed.getMasterInfo();
      masterInfo.set(newMasterInfo);
      startup.startup(newMasterInfo);
      state = SchedulerState.SUBSCRIBED;
      queuedUpdates.forEach(this::handleStatusUpdateAsync);
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
    lastOfferTimestamp = Optional.of(System.currentTimeMillis());
    try {
      lock.runWithOffersLock(() -> offerScheduler.resourceOffers(offers), "SingularityMesosScheduler");
    } catch (Throwable t) {
      LOG.error("Scheduler threw an uncaught exception - exiting", t);
      exceptionNotifier.notify(String.format("Scheduler threw an uncaught exception (%s)", t.getMessage()), t);
      notifyStopping();
      abort.abort(AbortReason.UNRECOVERABLE_ERROR, Optional.of(t));
    }
  }

  @Override
  public void inverseOffers(List<InverseOffer> offers) {
    LOG.debug("Singularity is currently not able to handle inverse offers events");
  }

  @Override
  public void rescind(OfferID offerId) {
    if (!isRunning()) {
      LOG.warn("Received rescind when not running for offer {}", offerId.getValue());
    }
    callWithOffersLock(() -> offerCache.rescindOffer(offerId), "rescind");
  }

  @Override
  public void rescindInverseOffer(OfferID offerId) {
    LOG.debug("Singularity is currently not able to handle inverse offers events");
  }

  @Override
  public CompletableFuture<Boolean> statusUpdate(TaskStatus status) {
    if (!isRunning()) {
      LOG.info("Scheduler is in state {}, queueing an update {} - {} queued updates so far", state.name(), status, queuedUpdates.size());
      queuedUpdates.add(status);
      return CompletableFuture.completedFuture(false);
    }
    try {
      return handleStatusUpdateAsync(status)
          .thenApply((r) -> r == StatusUpdateResult.DONE);
    } catch (Throwable t) {
      LOG.error("Scheduler threw an uncaught exception", t);
      notifyStopping();
      abort.abort(AbortReason.UNRECOVERABLE_ERROR, Optional.of(t));
      return CompletableFuture.completedFuture(false);
    }
  }

  @Override
  public void message(Message message) {
    ExecutorID executorID = message.getExecutorId();
    AgentID slaveId = message.getAgentId();
    byte[] data = message.getData().toByteArray();
    LOG.info("Framework message from executor {} on slave {} with {} bytes of data", executorID, slaveId, data.length);
    messageHandler.handleMessage(executorID, slaveId, data);
  }

  @Override
  public void failure(Failure failure) {
    if (failure.hasExecutorId()) {
      LOG.warn("Lost an executor {} on slave {} with status {}", failure.getExecutorId(), failure.getAgentId(), failure.getStatus());
    } else {
      slaveLost(failure.getAgentId());
    }
  }

  @Override
  public void error(String message) {
    callWithStateLock(() -> {
      LOG.error("Aborting due to error: {}", message);
      notifyStopping();
      abort.abort(AbortReason.MESOS_ERROR, Optional.absent());
    }, "error", true);
  }

  @Override
  public void heartbeat(Event event) {
    long now = System.currentTimeMillis();
    long delta = (now - lastHeartbeatTime.getAndSet(now));
    LOG.debug("Heartbeat from mesos. Delta since last heartbeat is {}ms", delta);
  }

  @Override
  public void onUncaughtException(Throwable t) {
    LOG.error("uncaught exception", t);
    callWithStateLock(() -> {
      if (t instanceof PrematureChannelClosureException) {
        LOG.error("Lost connection to the mesos master, aborting", t);
        notifyStopping();
        abort.abort(AbortReason.LOST_MESOS_CONNECTION, Optional.of(t));
      } else {
        LOG.error("Aborting due to error: {}", t.getMessage(), t);
        notifyStopping();
        abort.abort(AbortReason.MESOS_ERROR, Optional.of(t));
      }
    }, "errorUncaughtException", true);
  }

  @Override
  public void onConnectException(Throwable t) {
    callWithStateLock(() -> {
      LOG.error("Unable to connect to mesos master {}", t.getMessage(), t);
      try {
        start();
      } catch (Throwable startThrowable) {
        LOG.error("Unable to retry mesos master connection", startThrowable);
        notifyStopping();
        abort.abort(AbortReason.MESOS_ERROR, Optional.of(startThrowable));
      }
    }, "errorConnectException", false);
  }

  @Override
  public long getEventBufferSize() {
    return configuration.getMesosConfiguration().getRxEventBufferSize();
  }

  public void start() throws Exception {
    MesosConfiguration mesosConfiguration = configuration.getMesosConfiguration();
    // If more than one host is provided choose at random, we will be redirected if the host is not the master
    List<String> masters = Arrays.asList(mesosConfiguration.getMaster().split(","));
    String nextMaster = masters.get(new Random().nextInt(masters.size()));
    if (!nextMaster.startsWith("http")) {
      nextMaster = "http://" + nextMaster;
    }
    URI masterUri = URI.create(nextMaster);

    String userInfo = masterUri.getUserInfo();
    if (userInfo == null && mesosConfiguration.getMesosUsername().isPresent() && mesosConfiguration.getMesosPassword().isPresent()) {
      userInfo = String.format("%s:%s", mesosConfiguration.getMesosUsername().get(), mesosConfiguration.getMesosPassword().get());
    }

    mesosSchedulerClient.subscribe(new URI(
        masterUri.getScheme() == null ? "http" : masterUri.getScheme(),
        userInfo,
        masterUri.getHost(),
        masterUri.getPort(),
        Strings.isNullOrEmpty(masterUri.getPath()) ? "/api/v1/scheduler" : masterUri.getPath(),
        masterUri.getQuery(),
        masterUri.getFragment()
    ), this);
  }

  private void callWithOffersLock(Runnable function, String method) {
    if (!isRunning()) {
      LOG.info("Ignoring {} because scheduler isn't running ({})", method, state);
      return;
    }
    try {
      lock.runWithOffersLock(function, String.format("%s#%s", getClass().getSimpleName(), method));
    } catch (Throwable t) {
      LOG.error("Scheduler threw an uncaught exception - exiting", t);
      exceptionNotifier.notify(String.format("Scheduler threw an uncaught exception (%s)", t.getMessage()), t);
      notifyStopping();
      abort.abort(AbortReason.UNRECOVERABLE_ERROR, Optional.of(t));
    }
  }

  private void callWithStateLock(Runnable function, String name, boolean ignoreIfNotRunning) {
    if (ignoreIfNotRunning && !isRunning()) {
      LOG.info("Ignoring {} because scheduler isn't running ({})", name, state);
      return;
    }

    try {
      lock.runWithStateLock(function, name);
    } catch (Throwable t) {
      LOG.error("Scheduler threw an uncaught exception - exiting", t);
      exceptionNotifier.notify(String.format("Scheduler threw an uncaught exception (%s)", t.getMessage()), t);
      notifyStopping();
      abort.abort(AbortReason.UNRECOVERABLE_ERROR, Optional.of(t));
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
    callWithStateLock(() -> state = SchedulerState.SUBSCRIBED, "setSubscribed", false);
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

  public Optional<Double> getHeartbeatIntervalSeconds() {
    return heartbeatIntervalSeconds;
  }

  public void killAndRecord(SingularityTaskId taskId, Optional<RequestCleanupType> requestCleanupType, Optional<TaskCleanupType> taskCleanupType, Optional<Long> originalTimestamp, Optional<Integer> retries, Optional<String> user) {
    Preconditions.checkState(isRunning());

    Optional<TaskCleanupType> maybeCleanupFromRequestAndTask = getTaskCleanupType(requestCleanupType, taskCleanupType);

    if (maybeCleanupFromRequestAndTask.isPresent() && (maybeCleanupFromRequestAndTask.get() == TaskCleanupType.USER_REQUESTED_DESTROY || maybeCleanupFromRequestAndTask.get() == TaskCleanupType.REQUEST_DELETING)) {
      Optional<SingularityTask> task = taskManager.getTask(taskId);
      if (task.isPresent()) {
        if (task.get().getTaskRequest().getDeploy().getCustomExecutorCmd().isPresent()) {
          byte[] messageBytes = transcoder.toBytes(new SingularityTaskDestroyFrameworkMessage(taskId, user));
          mesosSchedulerClient.frameworkMessage(
              MesosProtosUtils.toExecutorId(task.get().getMesosTask().getExecutor().getExecutorId()),
              MesosProtosUtils.toAgentId(task.get().getMesosTask().getAgentId()),
              messageBytes
          );
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

  private CompletableFuture<StatusUpdateResult> handleStatusUpdateAsync(TaskStatus status) {
    long start = System.currentTimeMillis();
    return statusUpdateHandler.processStatusUpdateAsync(status)
        .whenCompleteAsync((result, throwable) -> {
          if (throwable != null) {
            LOG.error("Scheduler threw an uncaught exception processing status updates", throwable);
            notifyStopping();
            abort.abort(AbortReason.UNRECOVERABLE_ERROR, Optional.of(throwable));
          }
          if (status.hasUuid()) {
            mesosSchedulerClient.acknowledge(status.getAgentId(), status.getTaskId(), status.getUuid());
          }
          if (result == StatusUpdateResult.KILL_TASK) {
            LOG.info("Killing a task {} which Singularity has no remaining active state for. It will be given 1 minute to shut down gracefully", status.getTaskId().getValue());
            mesosSchedulerClient.kill(status.getTaskId(), status.getAgentId(),
                KillPolicy.newBuilder().setGracePeriod(DurationInfo.newBuilder().setNanoseconds(TimeUnit.MINUTES.toNanos(1)).build()).build());
          }
          LOG.debug("Handled status update for {} in {}", status.getTaskId().getValue(), JavaUtils.duration(start));
        });
  }
}
