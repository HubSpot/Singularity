package com.hubspot.singularity.mesos;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.client.MesosClient;
import com.hubspot.mesos.json.MesosMasterStateObject;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskHistoryUpdate.SimplifiedTaskState;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdHolder;
import com.hubspot.singularity.async.CompletableFutures;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.DisasterManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.zkmigrations.ZkDataMigrationRunner;
import com.hubspot.singularity.helpers.MesosUtils;
import com.hubspot.singularity.scheduler.SingularityHealthchecker;
import com.hubspot.singularity.scheduler.SingularityNewTaskChecker;
import com.hubspot.singularity.scheduler.SingularityTaskReconciliation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Singleton;
import org.apache.mesos.v1.Protos.MasterInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
class SingularityStartup {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityStartup.class);

  private final MesosClient mesosClient;
  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final DeployManager deployManager;
  private final DisasterManager disasterManager;
  private final SingularityAgentAndRackManager agentAndRackManager;
  private final SingularityHealthchecker healthchecker;
  private final SingularityNewTaskChecker newTaskChecker;
  private final SingularityTaskReconciliation taskReconciliation;
  private final ZkDataMigrationRunner zkDataMigrationRunner;
  private final SingularitySchedulerLock lock;
  private final SingularityConfiguration configuration;

  @Inject
  SingularityStartup(
    MesosClient mesosClient,
    SingularityHealthchecker healthchecker,
    SingularityNewTaskChecker newTaskChecker,
    SingularityAgentAndRackManager agentAndRackManager,
    TaskManager taskManager,
    RequestManager requestManager,
    DeployManager deployManager,
    DisasterManager disasterManager,
    SingularityTaskReconciliation taskReconciliation,
    ZkDataMigrationRunner zkDataMigrationRunner,
    SingularitySchedulerLock lock,
    SingularityConfiguration configuration
  ) {
    this.mesosClient = mesosClient;
    this.zkDataMigrationRunner = zkDataMigrationRunner;
    this.agentAndRackManager = agentAndRackManager;
    this.deployManager = deployManager;
    this.disasterManager = disasterManager;
    this.requestManager = requestManager;
    this.newTaskChecker = newTaskChecker;
    this.taskManager = taskManager;
    this.healthchecker = healthchecker;
    this.taskReconciliation = taskReconciliation;
    this.lock = lock;
    this.configuration = configuration;
  }

  public void checkMigrations() {
    zkDataMigrationRunner.checkMigrations();
  }

  public void startup(MasterInfo masterInfo) {
    final long start = System.currentTimeMillis();

    final String uri = mesosClient.getMasterUri(
      MesosUtils.getMasterHostAndPort(masterInfo)
    );

    LOG.info("Starting up... fetching state data from: " + uri);

    MesosMasterStateObject state = mesosClient.getMasterState(uri);

    agentAndRackManager.loadAgentsAndRacksFromMaster(state, true); // TODO: is this correctly doing host reconciliation?

    ExecutorService startupExecutor = Executors.newFixedThreadPool(
      configuration.getSchedulerStartupConcurrency(),
      new ThreadFactoryBuilder().setNameFormat("startup-%d").build()
    );

    List<CompletableFuture<Void>> checkFutures = checkSchedulerForInconsistentState(
      startupExecutor
    );

    CompletableFutures.allOf(enqueueHealthAndNewTaskChecks(startupExecutor)).join();
    CompletableFutures.allOf(checkFutures).join();

    startupExecutor.shutdown();

    if (!disasterManager.isDisabled(SingularityAction.STARTUP_TASK_RECONCILIATION)) {
      taskReconciliation.startReconciliation();
    }

    LOG.info("Finished startup after {}", JavaUtils.duration(start));
  }

  private Map<SingularityDeployKey, SingularityPendingTaskId> getDeployKeyToPendingTaskId() {
    final List<SingularityPendingTaskId> pendingTaskIds = taskManager.getPendingTaskIds();
    final Map<SingularityDeployKey, SingularityPendingTaskId> deployKeyToPendingTaskId = Maps.newHashMapWithExpectedSize(
      pendingTaskIds.size()
    );

    for (SingularityPendingTaskId taskId : pendingTaskIds) {
      SingularityDeployKey deployKey = new SingularityDeployKey(
        taskId.getRequestId(),
        taskId.getDeployId()
      );
      deployKeyToPendingTaskId.put(deployKey, taskId);
    }

    return deployKeyToPendingTaskId;
  }

  /**
   * We need to run this check for the various situations where the scheduler could get in an inconsistent state due
   * to a crash/network failure during series of state transactions.
   *
   *  1) Unpausing
   *  2) Launching Task
   *
   */
  @VisibleForTesting
  List<CompletableFuture<Void>> checkSchedulerForInconsistentState(
    ExecutorService startupExecutor
  ) {
    final long now = System.currentTimeMillis();

    final Map<SingularityDeployKey, SingularityPendingTaskId> deployKeyToPendingTaskId = getDeployKeyToPendingTaskId();

    List<CompletableFuture<Void>> checkFutures = new ArrayList<>();
    for (String requestId : requestManager.getAllRequestIds()) {
      checkFutures.add(
        CompletableFuture.runAsync(
          () ->
            lock.runWithRequestLock(
              () -> {
                Optional<SingularityRequestWithState> maybeWithState = requestManager.getRequest(
                  requestId
                );
                if (maybeWithState.isPresent()) {
                  switch (maybeWithState.get().getState()) {
                    case ACTIVE:
                    case SYSTEM_COOLDOWN:
                    case DEPLOYING_TO_UNPAUSE:
                      checkActiveRequest(
                        maybeWithState.get(),
                        deployKeyToPendingTaskId,
                        now
                      );
                      break;
                    case DELETED:
                    case PAUSED:
                    case FINISHED:
                      break;
                  }
                }
              },
              requestId,
              "startup"
            ),
          startupExecutor
        )
      );
    }
    return checkFutures;
  }

  private void checkActiveRequest(
    SingularityRequestWithState requestWithState,
    Map<SingularityDeployKey, SingularityPendingTaskId> deployKeyToPendingTaskId,
    final long timestamp
  ) {
    final SingularityRequest request = requestWithState.getRequest();

    if (
      request.getRequestType() == RequestType.ON_DEMAND ||
      request.getRequestType() == RequestType.RUN_ONCE
    ) {
      return; // There's no situation where we'd want to schedule an On Demand or Run Once request at startup, so don't even bother with them.
    }

    Optional<SingularityRequestDeployState> requestDeployState = deployManager.getRequestDeployState(
      request.getId()
    );

    if (
      !requestDeployState.isPresent() ||
      !requestDeployState.get().getActiveDeploy().isPresent()
    ) {
      LOG.debug("No active deploy for {} - not scheduling on startup", request.getId());
      return;
    }

    final String activeDeployId = requestDeployState
      .get()
      .getActiveDeploy()
      .get()
      .getDeployId();

    if (request.isScheduled()) {
      SingularityDeployKey deployKey = new SingularityDeployKey(
        request.getId(),
        activeDeployId
      );
      SingularityPendingTaskId pendingTaskId = deployKeyToPendingTaskId.get(deployKey);

      if (
        pendingTaskId != null &&
        pendingTaskId.getCreatedAt() >= requestWithState.getTimestamp()
      ) {
        LOG.info(
          "Not rescheduling {} because {} is newer than {}",
          request.getId(),
          pendingTaskId,
          requestWithState.getTimestamp()
        );
        return;
      }
    }

    requestManager.addToPendingQueue(
      new SingularityPendingRequest(
        request.getId(),
        activeDeployId,
        timestamp,
        Optional.<String>empty(),
        PendingType.STARTUP,
        Optional.<Boolean>empty(),
        Optional.<String>empty()
      )
    );
  }

  private List<CompletableFuture<Void>> enqueueHealthAndNewTaskChecks(
    ExecutorService startupExecutor
  ) {
    final List<SingularityTask> activeTasks = taskManager.getActiveTasks();
    final Map<SingularityTaskId, SingularityTask> activeTaskMap = Maps.uniqueIndex(
      activeTasks,
      SingularityTaskIdHolder.getTaskIdFunction()
    );

    final Map<SingularityTaskId, List<SingularityTaskHistoryUpdate>> taskUpdates = taskManager.getTaskHistoryUpdates(
      activeTaskMap.keySet()
    );

    final Map<SingularityDeployKey, SingularityPendingDeploy> pendingDeploys = Maps.uniqueIndex(
      deployManager.getPendingDeploys(),
      SingularityDeployKey.FROM_PENDING_TO_DEPLOY_KEY::apply
    );
    final Map<String, SingularityRequestWithState> idToRequest = Maps.uniqueIndex(
      requestManager.getRequests(),
      SingularityRequestWithState.REQUEST_STATE_TO_REQUEST_ID::apply
    );

    AtomicInteger enqueuedNewTaskChecks = new AtomicInteger(0);
    AtomicInteger enqueuedHealthchecks = new AtomicInteger(0);

    List<CompletableFuture<Void>> enqueueFutures = new ArrayList<>();
    for (Entry<SingularityTaskId, SingularityTask> entry : activeTaskMap.entrySet()) {
      enqueueFutures.add(
        CompletableFuture.runAsync(
          () -> {
            SingularityTaskId taskId = entry.getKey();
            SingularityTask task = entry.getValue();
            SimplifiedTaskState simplifiedTaskState = SingularityTaskHistoryUpdate.getCurrentState(
              taskUpdates.get(taskId)
            );

            if (simplifiedTaskState != SimplifiedTaskState.DONE) {
              SingularityDeployKey deployKey = new SingularityDeployKey(
                taskId.getRequestId(),
                taskId.getDeployId()
              );
              Optional<SingularityPendingDeploy> pendingDeploy = Optional.ofNullable(
                pendingDeploys.get(deployKey)
              );
              Optional<SingularityRequestWithState> request = Optional.ofNullable(
                idToRequest.get(taskId.getRequestId())
              );

              if (!pendingDeploy.isPresent()) {
                newTaskChecker.enqueueNewTaskCheck(task, request, healthchecker);
                enqueuedNewTaskChecks.getAndIncrement();
              }
              if (simplifiedTaskState == SimplifiedTaskState.RUNNING) {
                if (healthchecker.enqueueHealthcheck(task, pendingDeploy, request)) {
                  enqueuedHealthchecks.getAndIncrement();
                }
              }
            }
          },
          startupExecutor
        )
      );
    }

    LOG.info(
      "Enqueued {} health checks and {} new task checks (out of {} active tasks)",
      enqueuedHealthchecks.get(),
      enqueuedNewTaskChecks.get(),
      activeTasks.size()
    );
    return enqueueFutures;
  }
}
