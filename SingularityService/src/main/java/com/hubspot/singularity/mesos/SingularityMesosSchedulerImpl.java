package com.hubspot.singularity.mesos;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.apache.curator.framework.state.ConnectionState;
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
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.RequestCleanupType;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityAbort.AbortReason;
import com.hubspot.singularity.SingularityKilledTaskIdRecord;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.SingularityManagedThreadPoolFactory;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskDestroyFrameworkMessage;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.transcoders.Transcoder;
import com.hubspot.singularity.helpers.MesosProtosUtils;
import com.hubspot.singularity.mesos.SchedulerState.MesosSchedulerState;
import com.hubspot.singularity.scheduler.SingularityLeaderCacheCoordinator;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
  private final ExecutorService offerExecutor;
  private final ExecutorService subscribeExecutor;

  private volatile SchedulerState state;
  private volatile Optional<Long> lastOfferTimestamp = Optional.empty();
  private Optional<Double> heartbeatIntervalSeconds = Optional.empty();

  private final AtomicReference<MasterInfo> masterInfo = new AtomicReference<>();
  private final StatusUpdateQueue queuedUpdates;
  private final ExecutorService reconnectExecutor;
  private final AtomicInteger reconnectAttempts;

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
                                SingularityConfiguration configuration,
                                TaskManager taskManager,
                                Transcoder<SingularityTaskDestroyFrameworkMessage> transcoder,
                                StatusUpdateQueue queuedUpdates,
                                SingularityManagedThreadPoolFactory threadPoolFactory,
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
    this.queuedUpdates = queuedUpdates;
    this.lock = lock;
    this.offerExecutor = threadPoolFactory.get("offer-scheduler", 2);
    this.subscribeExecutor = threadPoolFactory.getSingleThreaded("subscribe-scheduler");
    this.state = new SchedulerState();
    this.configuration = configuration;
    this.reconnectExecutor = threadPoolFactory.getSingleThreaded("reconnect-scheduler");
    this.reconnectAttempts = new AtomicInteger(0);
  }

  @Override
  public CompletableFuture<Void> subscribed(Subscribed subscribed) {
    return CompletableFuture.runAsync(() ->
        callWithStateLock(() -> {
          reconnectAttempts.set(0);
          MasterInfo newMasterInfo = subscribed.getMasterInfo();
          masterInfo.set(newMasterInfo);
          Preconditions.checkState(state.getMesosSchedulerState() != MesosSchedulerState.SUBSCRIBED, "Asked to startup - but in invalid state: %s", state.getMesosSchedulerState());

          double advertisedHeartbeatIntervalSeconds = subscribed.getHeartbeatIntervalSeconds();
          if (advertisedHeartbeatIntervalSeconds > 0) {
            heartbeatIntervalSeconds = Optional.of(advertisedHeartbeatIntervalSeconds);
          }

          if (state.getMesosSchedulerState() != MesosSchedulerState.PAUSED_FOR_MESOS_RECONNECT) {
            // Should be called before activation of leader cache or cache could be left empty
            startup.checkMigrations();
            leaderCacheCoordinator.activateLeaderCache();
          }
          startup.startup(newMasterInfo);
          state.setMesosSchedulerState(MesosSchedulerState.SUBSCRIBED);
          handleQueuedStatusUpdates();
        }, "subscribed", false),
        subscribeExecutor);
  }

  @Timed
  @Override
  @SuppressFBWarnings(value="NP_NONNULL_PARAM_VIOLATION") // it is valid to pass NULL to CompletableFuture#completedFuture
  public CompletableFuture<Void> resourceOffers(List<Offer> offers) {
    long received = System.currentTimeMillis();
    lastOfferTimestamp = Optional.of(received);
    if (!isRunning()) {
      LOG.info("Scheduler is in state {}, declining {} offer(s)", state.getMesosSchedulerState(), offers.size());
      mesosSchedulerClient.decline(offers.stream().map(Offer::getId).collect(Collectors.toList()));
      return CompletableFuture.completedFuture(null);
    }
    return CompletableFuture.runAsync(() -> {
      try {
        long lag = System.currentTimeMillis() - received;
        if (lag > configuration.getMesosConfiguration().getOfferTimeout()) {
          LOG.info("Offer lag {} too large, declining {} offers", lag, offers.size());
          mesosSchedulerClient.decline(offers.stream().map(Offer::getId).collect(Collectors.toList()));
          return;
        } else {
          LOG.info("Starting offer check after {}ms", lag);
        }
        lock.runWithOffersLockAndtimeout((acquiredLock) -> {
          if (acquiredLock) {
            offerScheduler.resourceOffers(offers);
          } else {
            LOG.info("Did not acquire offer lock in time, will cache offers");
            offers.forEach((o) -> offerCache.cacheOffer(received, o));
          }
          return null;
        }, "SingularityMesosScheduler", configuration.getMesosConfiguration().getOfferLockTimeout());
      } catch (Throwable t) {
        LOG.error("Scheduler threw an uncaught exception - exiting", t);
        exceptionNotifier.notify(String.format("Scheduler threw an uncaught exception (%s)", t.getMessage()), t);
        notifyStopping();
        abort.abort(AbortReason.UNRECOVERABLE_ERROR, Optional.of(t));
      }
    }, offerExecutor);
  }

  @Override
  public void inverseOffers(List<InverseOffer> offers) {
    LOG.debug("Singularity is currently not able to handle inverse offers events");
  }

  @Override
  public CompletableFuture<Void> rescind(OfferID offerId) {
    if (!isRunning()) {
      LOG.warn("Received rescind when not running for offer {}", offerId.getValue());
    }
    return CompletableFuture.runAsync(() -> callWithOffersLock(() -> offerCache.rescindOffer(offerId), "rescind"), offerExecutor);
  }

  @Override
  public void rescindInverseOffer(OfferID offerId) {
    LOG.debug("Singularity is currently not able to handle inverse offers events");
  }

  @Override
  public CompletableFuture<Boolean> statusUpdate(TaskStatus status) {
    lastHeartbeatTime.getAndSet(System.currentTimeMillis()); // Consider status update a heartbeat, we are still getting valid communication from mesos
    if (!state.isRunning()) {
      try {
        LOG.trace("Scheduler is in state {}, queueing an update {} - {} queued updates so far", state.getMesosSchedulerState(), status, queuedUpdates.size());
        queuedUpdates.add(status);
        return CompletableFuture.completedFuture(false);
      } catch (IOException ioe) {
        LOG.error("Unable to queue status update", ioe);
        notifyStopping();
        abort.abort(AbortReason.UNRECOVERABLE_ERROR, Optional.of(ioe));
        return CompletableFuture.completedFuture(false);
      }
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
      abort.abort(AbortReason.MESOS_ERROR, Optional.empty());
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
        reconnectMesos();
      } else {
        LOG.error("Aborting due to error: {}", t.getMessage(), t);
        notifyStopping();
        abort.abort(AbortReason.MESOS_ERROR, Optional.of(t));
      }
    }, "errorUncaughtException", true);
  }

  @Override
  public void onConnectException(Throwable t) {
    reconnectMesos();
  }

  @Override
  public long getEventBufferSize() {
    return configuration.getMesosConfiguration().getRxEventBufferSize();
  }

  public void start() throws Exception {
    callWithStateLock(() -> {
      MesosSchedulerState currentState = state.getMesosSchedulerState();
      if (currentState == MesosSchedulerState.SUBSCRIBED) {
        LOG.info("Already connected to mesos, will not reconnect");
        return;
      } else if (currentState == MesosSchedulerState.PAUSED_SUBSCRIBED) {
        LOG.info("Already subscribed, restarting scheduler actions");
        state.setMesosSchedulerState(MesosSchedulerState.SUBSCRIBED);
        handleQueuedStatusUpdates();
        return;
      }

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

      try {
        mesosSchedulerClient.subscribe(new URI(
            masterUri.getScheme() == null ? "http" : masterUri.getScheme(),
            userInfo,
            masterUri.getHost(),
            masterUri.getPort(),
            Strings.isNullOrEmpty(masterUri.getPath()) ? "/api/v1/scheduler" : masterUri.getPath(),
            masterUri.getQuery(),
            masterUri.getFragment()
        ), this);
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }, "start", false);
  }

  private void callWithOffersLock(Runnable function, String method) {
    if (!state.isRunning()) {
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
    if (ignoreIfNotRunning && !state.isRunning()) {
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

  public void reconnectMesos() {
    // Done on a separate thread so that possibly interrupting the subscriber thread will not loop around to call
    // this method again on the same thread, causing an InterruptException
    CompletableFuture.runAsync(this::reconnectMesosSync, reconnectExecutor);
  }

  public void reconnectMesosSync() {
    callWithStateLock(() -> {
      state.setMesosSchedulerState(MesosSchedulerState.PAUSED_FOR_MESOS_RECONNECT);
      LOG.info("Paused scheduler actions, closing existing mesos connection");
      mesosSchedulerClient.close();
      LOG.info("Closed existing mesos connection");
      try {
        if (reconnectAttempts.getAndIncrement() > 0) {
          Thread.sleep(reconnectAttempts.get() * 3000);
        }
        Retryer<Void> startRetryer = RetryerBuilder.<Void>newBuilder()
            .retryIfException()
            .retryIfRuntimeException()
            .withWaitStrategy(WaitStrategies.exponentialWait())
            .withStopStrategy(StopStrategies.stopAfterDelay(configuration.getMesosConfiguration().getReconnectTimeoutMillis(), TimeUnit.MILLISECONDS))
            .build();
        startRetryer.call(() -> {
          start();
          return null;
        });
      } catch (RetryException re) {
        if (re.getLastFailedAttempt().getExceptionCause() != null) {
          LOG.error("Unable to retry mesos master connection", re.getLastFailedAttempt().getExceptionCause());
          notifyStopping();
          abort.abort(AbortReason.MESOS_ERROR, Optional.of(re.getLastFailedAttempt().getExceptionCause()));
        }
      } catch (Throwable t) {
        LOG.error("Unable to retry mesos master connection", t);
        notifyStopping();
        abort.abort(AbortReason.MESOS_ERROR, Optional.of(t));
      }
    }, "reconnectMesos", false);
  }

  public void setZkConnectionState(ConnectionState connectionState) {
    state.setZkConnectionState(connectionState);
  }

  public void notLeader() {
    callWithStateLock(() -> {
      if (state.getMesosSchedulerState() != MesosSchedulerState.STOPPED) {
        state.setMesosSchedulerState(MesosSchedulerState.STOPPED);
        LOG.info("Stopping and clearing leader cache");
        leaderCacheCoordinator.stopLeaderCache();
        leaderCacheCoordinator.clear();
        LOG.info("Closing any open mesos master connections");
        mesosSchedulerClient.close();
        LOG.info("Scheduler now in state: {}", state);
      }
    }, "notLeader", false);
  }

  public void pauseForDatastoreReconnect() {
    callWithStateLock(() -> {
      if (state.getMesosSchedulerState() == MesosSchedulerState.SUBSCRIBED) {
        state.setMesosSchedulerState(MesosSchedulerState.PAUSED_SUBSCRIBED);
      }
    }, "pause", false);
  }

  public void notifyStopping() {
    LOG.info("Scheduler is moving to stopped, current state: {}", state);

    state.setMesosSchedulerState(MesosSchedulerState.STOPPED);

    leaderCacheCoordinator.stopLeaderCache();
    mesosSchedulerClient.close();

    LOG.info("Scheduler now in state: {}", state);
  }

  public boolean isRunning() {
    return state.isRunning();
  }

  // Only used in unit tests
  public void setSubscribed() {
    callWithStateLock(() -> state.setMesosSchedulerState(MesosSchedulerState.SUBSCRIBED), "setSubscribed", false);
  }

  public Optional<MasterInfo> getMaster() {
    return Optional.ofNullable(masterInfo.get());
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

    taskManager.saveKilledRecord(new SingularityKilledTaskIdRecord(taskId, System.currentTimeMillis(), originalTimestamp.orElse(System.currentTimeMillis()), requestCleanupType, taskCleanupType, retries.orElse(-1) + 1));
  }

  private Optional<TaskCleanupType> getTaskCleanupType(Optional<RequestCleanupType> requestCleanupType, Optional<TaskCleanupType> taskCleanupType) {
    if (taskCleanupType.isPresent()) {
      return taskCleanupType;
    } else {
      if (requestCleanupType.isPresent()) {
        return requestCleanupType.get().getTaskCleanupType();
      }
      return Optional.empty();
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
            boolean isZkException = Throwables.getCausalChain(throwable).stream().anyMatch((t) -> t instanceof KeeperException);
            if (isZkException) {
              LOG.info("Not aborting for KeeperException. Leaving status update unacked for {}", status.getTaskId());
              return;
            }
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

  private void handleQueuedStatusUpdates() {
    try {
      Iterator<TaskStatus> diskQueueIterator = queuedUpdates.diskQueueIterator();
      while (diskQueueIterator.hasNext()) {
        while (!statusUpdateHandler.hasRoomForMoreUpdates()) {
          LOG.debug("Status update queue is full, waiting before processing additional updates");
          Thread.sleep(2000);
        }
        TaskStatus status = diskQueueIterator.next();
        handleStatusUpdateAsync(status);
        diskQueueIterator.remove();
      }

      TaskStatus nextInMemory = queuedUpdates.nextInMemory();
      while (nextInMemory != null) {
        while (!statusUpdateHandler.hasRoomForMoreUpdates()) {
          LOG.debug("Status update queue is full, waiting before processing additional updates");
          Thread.sleep(2000);
        }
        handleStatusUpdateAsync(nextInMemory);
        nextInMemory = queuedUpdates.nextInMemory();
      }
    } catch (Throwable t) {
      LOG.error("Unable to process queued status updates", t);
      throw new RuntimeException(t);
    }
  }
}
