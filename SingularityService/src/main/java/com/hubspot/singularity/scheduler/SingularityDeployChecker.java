package com.hubspot.singularity.scheduler;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.DeployAcceptanceResult;
import com.hubspot.singularity.DeployProgressLbUpdateHolder;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.LoadBalancerRequestState;
import com.hubspot.singularity.LoadBalancerRequestType;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployFailure;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityDeployProgress;
import com.hubspot.singularity.SingularityDeployResult;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityManagedThreadPoolFactory;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingRequestBuilder;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityUpdatePendingDeployRequest;
import com.hubspot.singularity.api.SingularityRunNowRequest;
import com.hubspot.singularity.async.CompletableFutures;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.usage.UsageManager;
import com.hubspot.singularity.expiring.SingularityExpiringPause;
import com.hubspot.singularity.expiring.SingularityExpiringScale;
import com.hubspot.singularity.hooks.LoadBalancerClient;
import com.hubspot.singularity.mesos.SingularitySchedulerLock;
import com.hubspot.singularity.scheduler.SingularityDeployHealthHelper.DeployHealth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SingularityDeployChecker {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityDeployChecker.class
  );

  private final DeployManager deployManager;
  private final TaskManager taskManager;
  private final SingularityDeployHealthHelper deployHealthHelper;
  private final RequestManager requestManager;
  private final SingularityDeployCheckHelper deployCheckHelper;
  private final SingularityConfiguration configuration;
  private final LoadBalancerClient lbClient;
  private final SingularitySchedulerLock lock;
  private final UsageManager usageManager;
  private final SingularityDeployAcceptanceManager deployAcceptanceManager;
  private final ExecutorService deployCheckExecutor;

  @Inject
  public SingularityDeployChecker(
    DeployManager deployManager,
    SingularityDeployHealthHelper deployHealthHelper,
    LoadBalancerClient lbClient,
    RequestManager requestManager,
    TaskManager taskManager,
    SingularityDeployCheckHelper deployCheckHelper,
    SingularityConfiguration configuration,
    SingularitySchedulerLock lock,
    UsageManager usageManager,
    SingularityManagedThreadPoolFactory threadPoolFactory,
    SingularityDeployAcceptanceManager deployAcceptanceManager
  ) {
    this.configuration = configuration;
    this.lbClient = lbClient;
    this.deployHealthHelper = deployHealthHelper;
    this.requestManager = requestManager;
    this.deployManager = deployManager;
    this.taskManager = taskManager;
    this.lock = lock;
    this.usageManager = usageManager;
    this.deployCheckHelper = deployCheckHelper;
    this.deployAcceptanceManager = deployAcceptanceManager;
    this.deployCheckExecutor =
      threadPoolFactory.get("deploy-checker", configuration.getCoreThreadpoolSize());
  }

  public int checkDeploys() {
    final List<SingularityPendingDeploy> pendingDeploys = deployManager.getPendingDeploys();
    final List<SingularityDeployMarker> cancelDeploys = deployManager.getCancelDeploys();
    final List<SingularityUpdatePendingDeployRequest> updateRequests = deployManager.getPendingDeployUpdates();

    if (pendingDeploys.isEmpty() && cancelDeploys.isEmpty()) {
      return 0;
    }

    CompletableFutures
      .allOf(
        pendingDeploys
          .stream()
          .map(
            pendingDeploy ->
              CompletableFuture.runAsync(
                () ->
                  lock.runWithRequestLock(
                    () -> checkDeploy(pendingDeploy, cancelDeploys, updateRequests),
                    pendingDeploy.getDeployMarker().getRequestId(),
                    getClass().getSimpleName()
                  ),
                deployCheckExecutor
              )
          )
          .collect(Collectors.toList())
      )
      .join();

    cancelDeploys.forEach(deployManager::deleteCancelDeployRequest);
    updateRequests.forEach(deployManager::deleteUpdatePendingDeployRequest);

    return pendingDeploys.size();
  }

  private void checkDeploy(
    final SingularityPendingDeploy pendingDeploy,
    final List<SingularityDeployMarker> cancelDeploys,
    List<SingularityUpdatePendingDeployRequest> updateRequests
  ) {
    Thread
      .currentThread()
      .setName(
        String.format(
          "%s-%s-check",
          pendingDeploy.getDeployMarker().getRequestId(),
          pendingDeploy.getDeployMarker().getDeployId()
        )
      );
    final SingularityDeployKey deployKey = SingularityDeployKey.fromDeployMarker(
      pendingDeploy.getDeployMarker()
    );
    final Optional<SingularityDeploy> deploy = deployManager.getDeploy(
      deployKey.getRequestId(),
      deployKey.getDeployId()
    );

    Optional<SingularityRequestWithState> maybeRequestWithState = requestManager.getRequest(
      pendingDeploy.getDeployMarker().getRequestId()
    );
    if (deployCheckHelper.isNotInDeployableState(maybeRequestWithState)) {
      LOG.warn(
        "Deploy {} request was {}, removing deploy",
        pendingDeploy,
        SingularityRequestWithState.getRequestState(maybeRequestWithState)
      );

      if (SingularityDeployCheckHelper.shouldCancelLoadBalancer(pendingDeploy)) {
        cancelLoadBalancer(pendingDeploy, SingularityDeployFailure.deployRemoved());
      }

      failPendingDeployDueToState(pendingDeploy, maybeRequestWithState, deploy);
      return;
    }

    final SingularityDeployMarker pendingDeployMarker = pendingDeploy.getDeployMarker();

    final Optional<SingularityDeployMarker> cancelRequest = SingularityDeployCheckHelper.findCancel(
      cancelDeploys,
      pendingDeployMarker
    );
    final Optional<SingularityUpdatePendingDeployRequest> updatePendingDeployRequest = SingularityDeployCheckHelper.findUpdateRequest(
      updateRequests,
      pendingDeploy
    );

    final SingularityRequestWithState requestWithState = maybeRequestWithState.get();
    final SingularityRequest request = pendingDeploy
      .getUpdatedRequest()
      .orElse(requestWithState.getRequest());

    final List<SingularityTaskId> requestTasks = taskManager.getTaskIdsForRequest(
      request.getId()
    );
    final List<SingularityTaskId> activeTasks = taskManager.filterActiveTaskIds(
      requestTasks
    );

    final List<SingularityTaskId> inactiveDeployMatchingTasks = new ArrayList<>(
      requestTasks.size()
    );

    for (SingularityTaskId taskId : requestTasks) {
      if (
        taskId.getDeployId().equals(pendingDeployMarker.getDeployId()) &&
        !activeTasks.contains(taskId)
      ) {
        inactiveDeployMatchingTasks.add(taskId);
      }
    }

    final List<SingularityTaskId> deployMatchingTasks = new ArrayList<>(
      activeTasks.size()
    );
    final List<SingularityTaskId> allOtherMatchingTasks = new ArrayList<>(
      activeTasks.size()
    );

    for (SingularityTaskId taskId : activeTasks) {
      if (taskId.getDeployId().equals(pendingDeployMarker.getDeployId())) {
        deployMatchingTasks.add(taskId);
      } else {
        allOtherMatchingTasks.add(taskId);
      }
    }

    SingularityDeployResult deployResult = getDeployResultSafe(
      request,
      requestWithState.getState(),
      cancelRequest,
      pendingDeploy,
      updatePendingDeployRequest,
      deploy,
      deployMatchingTasks,
      allOtherMatchingTasks,
      inactiveDeployMatchingTasks
    );

    LOG.info(
      "Deploy {} had result {} after {}",
      pendingDeployMarker,
      deployResult,
      JavaUtils.durationFromMillis(
        System.currentTimeMillis() - pendingDeployMarker.getTimestamp()
      )
    );

    if (deployResult.getDeployState() == DeployState.SUCCEEDED) {
      if (saveNewDeployState(pendingDeployMarker, Optional.of(pendingDeployMarker))) {
        if (request.getRequestType() == RequestType.ON_DEMAND) {
          deleteOrRecreatePendingTasks(pendingDeploy);
        } else if (request.getRequestType() != RequestType.RUN_ONCE) {
          deleteObsoletePendingTasks(pendingDeploy);
        }
        finishDeploy(
          requestWithState,
          deploy,
          pendingDeploy,
          allOtherMatchingTasks,
          deployResult
        );
        return;
      } else {
        LOG.warn(
          "Failing deploy {} because it failed to save deploy state",
          pendingDeployMarker
        );
        deployResult =
          new SingularityDeployResult(
            DeployState.FAILED_INTERNAL_STATE,
            Optional.of(
              String.format(
                "Deploy had state %s but failed to persist it correctly",
                deployResult.getDeployState()
              )
            ),
            SingularityDeployFailure.failedToSave(),
            deployResult.getTimestamp()
          );
      }
    } else if (!deployResult.getDeployState().isDeployFinished()) {
      return;
    }

    // success case is handled, handle failure cases:
    saveNewDeployState(pendingDeployMarker, Optional.empty());
    finishDeploy(
      requestWithState,
      deploy,
      pendingDeploy,
      deployMatchingTasks,
      deployResult
    );
  }

  private void deleteOrRecreatePendingTasks(SingularityPendingDeploy pendingDeploy) {
    List<SingularityPendingTaskId> obsoletePendingTasks = new ArrayList<>();

    taskManager
      .getPendingTaskIdsForRequest(pendingDeploy.getDeployMarker().getRequestId())
      .forEach(
        taskId -> {
          if (
            !taskId.getDeployId().equals(pendingDeploy.getDeployMarker().getDeployId())
          ) {
            if (taskId.getPendingType() == PendingType.ONEOFF) {
              Optional<SingularityPendingTask> maybePendingTask = taskManager.getPendingTask(
                taskId
              );
              if (maybePendingTask.isPresent()) {
                // Reschedule any user-initiated pending tasks under the new deploy
                SingularityPendingTask pendingTask = maybePendingTask.get();
                requestManager.addToPendingQueue(
                  SingularityDeployCheckHelper.buildPendingRequest(
                    pendingTask,
                    pendingDeploy
                  )
                );
              }
            }
            obsoletePendingTasks.add(taskId);
          }
        }
      );

    for (SingularityPendingTaskId pendingTaskId : obsoletePendingTasks) {
      LOG.debug("Deleting obsolete pending task {}", pendingTaskId.getId());
      taskManager.deletePendingTask(pendingTaskId);
    }
  }

  private void deleteObsoletePendingTasks(SingularityPendingDeploy pendingDeploy) {
    List<SingularityPendingTaskId> obsoletePendingTasks = taskManager
      .getPendingTaskIdsForRequest(pendingDeploy.getDeployMarker().getRequestId())
      .stream()
      .filter(
        taskId ->
          !taskId.getDeployId().equals(pendingDeploy.getDeployMarker().getDeployId())
      )
      .collect(Collectors.toList());

    for (SingularityPendingTaskId pendingTaskId : obsoletePendingTasks) {
      LOG.debug("Deleting obsolete pending task {}", pendingTaskId.getId());
      taskManager.deletePendingTask(pendingTaskId);
    }
  }

  private void updateLoadBalancerStateForTasks(
    Collection<SingularityTaskId> taskIds,
    LoadBalancerRequestType type,
    SingularityLoadBalancerUpdate update
  ) {
    for (SingularityTaskId taskId : taskIds) {
      taskManager.saveLoadBalancerState(taskId, type, update);
    }
  }

  private void cleanupTasks(
    SingularityPendingDeploy pendingDeploy,
    SingularityRequest request,
    DeployState deployState,
    Iterable<SingularityTaskId> tasksToKill
  ) {
    for (SingularityTaskId matchingTask : tasksToKill) {
      taskManager.saveTaskCleanup(
        new SingularityTaskCleanup(
          pendingDeploy.getDeployMarker().getUser(),
          SingularityDeployCheckHelper.getCleanupType(
            pendingDeploy,
            request,
            deployState
          ),
          System.currentTimeMillis(),
          matchingTask,
          Optional.of(
            String.format(
              "Deploy %s - %s",
              pendingDeploy.getDeployMarker().getDeployId(),
              deployState.name()
            )
          ),
          Optional.empty(),
          Optional.empty()
        )
      );
    }
  }

  private boolean saveNewDeployState(
    SingularityDeployMarker pendingDeployMarker,
    Optional<SingularityDeployMarker> newActiveDeploy
  ) {
    Optional<SingularityRequestDeployState> deployState = deployManager.getRequestDeployState(
      pendingDeployMarker.getRequestId()
    );

    if (!deployState.isPresent()) {
      LOG.error(
        "Expected deploy state for deploy marker: {} but didn't find it",
        pendingDeployMarker
      );
      return false;
    }

    deployManager.saveNewRequestDeployState(
      new SingularityRequestDeployState(
        deployState.get().getRequestId(),
        newActiveDeploy.isPresent()
          ? newActiveDeploy
          : deployState.get().getActiveDeploy(),
        Optional.empty()
      )
    );

    return true;
  }

  private void finishDeploy(
    SingularityRequestWithState requestWithState,
    Optional<SingularityDeploy> deploy,
    SingularityPendingDeploy pendingDeploy,
    Iterable<SingularityTaskId> tasksToKill,
    SingularityDeployResult deployResult
  ) {
    SingularityRequest request = requestWithState.getRequest();

    if (!request.isOneOff() && !(request.getRequestType() == RequestType.RUN_ONCE)) {
      cleanupTasks(pendingDeploy, request, deployResult.getDeployState(), tasksToKill);
    }

    if (deploy.isPresent() && deploy.get().getRunImmediately().isPresent()) {
      String requestId = deploy.get().getRequestId();
      String deployId = deploy.get().getId();
      SingularityRunNowRequest runNowRequest = deploy.get().getRunImmediately().get();
      List<SingularityTaskId> activeTasks = taskManager.getActiveTaskIdsForRequest(
        requestId
      );
      List<SingularityPendingTaskId> pendingTasks = taskManager.getPendingTaskIdsForRequest(
        requestId
      );

      SingularityPendingRequestBuilder builder = SingularityDeployCheckHelper.buildBasePendingRequest(
        request,
        deployId,
        deployResult,
        pendingDeploy,
        runNowRequest
      );
      PendingType pendingType = SingularityDeployCheckHelper.computePendingType(
        request,
        activeTasks,
        pendingTasks
      );
      if (pendingType != null) {
        builder.setPendingType(
          SingularityDeployCheckHelper.canceledOr(
            deployResult.getDeployState(),
            pendingType
          )
        );
        requestManager.addToPendingQueue(builder.build());
      } else {
        LOG.warn("Could not determine pending type for deploy {}.", deployId);
      }
    } else if (!request.isDeployable() && !request.isOneOff()) {
      PendingType pendingType = SingularityDeployCheckHelper.canceledOr(
        deployResult.getDeployState(),
        PendingType.NEW_DEPLOY
      );
      requestManager.addToPendingQueue(
        new SingularityPendingRequest(
          request.getId(),
          pendingDeploy.getDeployMarker().getDeployId(),
          deployResult.getTimestamp(),
          pendingDeploy.getDeployMarker().getUser(),
          pendingType,
          deploy.isPresent()
            ? deploy.get().getSkipHealthchecksOnDeploy()
            : Optional.empty(),
          pendingDeploy.getDeployMarker().getMessage()
        )
      );
    }

    if (deployResult.getDeployState() == DeployState.SUCCEEDED) {
      if (request.isDeployable() && !request.isOneOff()) {
        // remove the lock on bounces in case we deployed during a bounce
        requestManager.markBounceComplete(request.getId());
        requestManager.removeExpiringBounce(request.getId());
      }
      if (requestWithState.getState() == RequestState.FINISHED) {
        // A FINISHED request is moved to ACTIVE state so we can reevaluate the schedule
        requestManager.activate(
          request,
          RequestHistoryType.UPDATED,
          System.currentTimeMillis(),
          deploy.isPresent() ? deploy.get().getUser() : Optional.empty(),
          Optional.empty()
        );
      }
      // Clear utilization since a new deploy will update usage patterns
      // do this async so sql isn't on the main scheduling path for deploys
      CompletableFuture
        .runAsync(
          () -> usageManager.deleteRequestUtilization(request.getId()),
          deployCheckExecutor
        )
        .exceptionally(
          t -> {
            LOG.error("Could not clear usage data after new deploy", t);
            return null;
          }
        );
    }

    deployManager.saveDeployResult(pendingDeploy.getDeployMarker(), deploy, deployResult);

    if (
      request.isDeployable() &&
      (
        deployResult.getDeployState() == DeployState.CANCELED ||
        deployResult.getDeployState() == DeployState.FAILED ||
        deployResult.getDeployState() == DeployState.FAILED_INTERNAL_STATE ||
        deployResult.getDeployState() == DeployState.OVERDUE
      )
    ) {
      Optional<SingularityRequestDeployState> maybeRequestDeployState = deployManager.getRequestDeployState(
        request.getId()
      );
      if (
        maybeRequestDeployState.isPresent() &&
        maybeRequestDeployState.get().getActiveDeploy().isPresent() &&
        !(
          requestWithState.getState() == RequestState.PAUSED ||
          requestWithState.getState() == RequestState.DEPLOYING_TO_UNPAUSE
        )
      ) {
        requestManager.addToPendingQueue(
          new SingularityPendingRequest(
            request.getId(),
            maybeRequestDeployState.get().getActiveDeploy().get().getDeployId(),
            deployResult.getTimestamp(),
            pendingDeploy.getDeployMarker().getUser(),
            deployResult.getDeployState() == DeployState.CANCELED
              ? PendingType.DEPLOY_CANCELLED
              : PendingType.DEPLOY_FAILED,
            request.getSkipHealthchecks(),
            pendingDeploy.getDeployMarker().getMessage()
          )
        );
      }
    }

    if (deployResult.getDeployState() == DeployState.SUCCEEDED) {
      List<SingularityTaskId> newDeployCleaningTasks = taskManager
        .getCleanupTaskIds()
        .stream()
        .filter(
          t -> t.getDeployId().equals(pendingDeploy.getDeployMarker().getDeployId())
        )
        .collect(Collectors.toList());
      // Account for any bounce/decom that may have happened during the deploy
      if (!newDeployCleaningTasks.isEmpty()) {
        requestManager.addToPendingQueue(
          new SingularityPendingRequest(
            request.getId(),
            pendingDeploy.getDeployMarker().getDeployId(),
            deployResult.getTimestamp(),
            pendingDeploy.getDeployMarker().getUser(),
            PendingType.DEPLOY_FINISHED,
            request.getSkipHealthchecks(),
            pendingDeploy.getDeployMarker().getMessage()
          )
        );
      }
    }

    if (
      request.isDeployable() &&
      deployResult.getDeployState() == DeployState.SUCCEEDED &&
      requestWithState.getState() != RequestState.PAUSED
    ) {
      if (
        pendingDeploy.getDeployProgress().getTargetActiveInstances() !=
        request.getInstancesSafe()
      ) {
        requestManager.addToPendingQueue(
          new SingularityPendingRequest(
            request.getId(),
            pendingDeploy.getDeployMarker().getDeployId(),
            deployResult.getTimestamp(),
            pendingDeploy.getDeployMarker().getUser(),
            PendingType.UPDATED_REQUEST,
            request.getSkipHealthchecks(),
            pendingDeploy.getDeployMarker().getMessage()
          )
        );
      }
    }

    if (requestWithState.getState() == RequestState.DEPLOYING_TO_UNPAUSE) {
      if (deployResult.getDeployState() == DeployState.SUCCEEDED) {
        requestManager.activate(
          request,
          RequestHistoryType.DEPLOYED_TO_UNPAUSE,
          deployResult.getTimestamp(),
          pendingDeploy.getDeployMarker().getUser(),
          Optional.empty()
        );
        requestManager.deleteExpiringObject(
          SingularityExpiringPause.class,
          request.getId()
        );
      } else {
        requestManager.pause(
          request,
          deployResult.getTimestamp(),
          pendingDeploy.getDeployMarker().getUser(),
          Optional.empty()
        );
      }
    }

    if (
      pendingDeploy.getUpdatedRequest().isPresent() &&
      deployResult.getDeployState() == DeployState.SUCCEEDED
    ) {
      requestManager.update(
        pendingDeploy.getUpdatedRequest().get(),
        System.currentTimeMillis(),
        pendingDeploy.getDeployMarker().getUser(),
        Optional.empty()
      );
      requestManager.deleteExpiringObject(
        SingularityExpiringScale.class,
        request.getId()
      );
    }

    removePendingDeploy(pendingDeploy);
  }

  private void removePendingDeploy(SingularityPendingDeploy pendingDeploy) {
    deployManager.deletePendingDeploy(pendingDeploy.getDeployMarker().getRequestId());
  }

  private void failPendingDeployDueToState(
    SingularityPendingDeploy pendingDeploy,
    Optional<SingularityRequestWithState> maybeRequestWithState,
    Optional<SingularityDeploy> deploy
  ) {
    SingularityDeployResult deployResult = new SingularityDeployResult(
      DeployState.FAILED,
      String.format(
        "Request in state %s is not deployable",
        SingularityRequestWithState.getRequestState(maybeRequestWithState)
      )
    );
    if (!maybeRequestWithState.isPresent()) {
      deployManager.saveDeployResult(
        pendingDeploy.getDeployMarker(),
        deploy,
        deployResult
      );
      removePendingDeploy(pendingDeploy);
      return;
    }

    saveNewDeployState(pendingDeploy.getDeployMarker(), Optional.empty());
    finishDeploy(
      maybeRequestWithState.get(),
      deploy,
      pendingDeploy,
      Collections.emptyList(),
      deployResult
    );
  }

  private void updatePendingDeploy(
    SingularityPendingDeploy pendingDeploy,
    DeployState deployState,
    SingularityDeployProgress deployProgress
  ) {
    SingularityPendingDeploy copy = new SingularityPendingDeploy(
      pendingDeploy.getDeployMarker(),
      deployState,
      deployProgress,
      pendingDeploy.getUpdatedRequest()
    );

    deployManager.savePendingDeploy(copy);
  }

  private SingularityLoadBalancerUpdate sendCancelToLoadBalancer(
    SingularityPendingDeploy pendingDeploy
  ) {
    return lbClient.cancel(
      pendingDeploy
        .getDeployProgress()
        .getPendingLbUpdate()
        .get()
        .getLoadBalancerRequestId()
    );
  }

  private SingularityDeployResult cancelLoadBalancer(
    SingularityPendingDeploy pendingDeploy,
    List<SingularityDeployFailure> deployFailures
  ) {
    final SingularityLoadBalancerUpdate lbUpdate = sendCancelToLoadBalancer(
      pendingDeploy
    );

    final DeployState deployState = SingularityDeployCheckHelper.interpretLoadBalancerState(
      lbUpdate,
      DeployState.CANCELING
    );

    updatePendingDeploy(
      pendingDeploy,
      deployState,
      pendingDeploy.getDeployProgress().withPendingLbUpdate(lbUpdate)
    );
    return new SingularityDeployResult(deployState, lbUpdate, deployFailures);
  }

  private SingularityDeployResult getDeployResultSafe(
    final SingularityRequest request,
    final RequestState requestState,
    final Optional<SingularityDeployMarker> cancelRequest,
    final SingularityPendingDeploy pendingDeploy,
    final Optional<SingularityUpdatePendingDeployRequest> updatePendingDeployRequest,
    final Optional<SingularityDeploy> deploy,
    final Collection<SingularityTaskId> deployActiveTasks,
    final Collection<SingularityTaskId> otherActiveTasks,
    final Collection<SingularityTaskId> inactiveDeployMatchingTasks
  ) {
    try {
      return getDeployResult(
        request,
        requestState,
        cancelRequest,
        pendingDeploy,
        updatePendingDeployRequest,
        deploy,
        deployActiveTasks,
        otherActiveTasks,
        inactiveDeployMatchingTasks
      );
    } catch (Exception e) {
      LOG.error(
        "Uncaught exception processing deploy {} - {}",
        pendingDeploy.getDeployMarker().getRequestId(),
        pendingDeploy.getDeployMarker().getDeployId(),
        e
      );
      return new SingularityDeployResult(
        DeployState.FAILED_INTERNAL_STATE,
        String.format("Uncaught exception: %s", e.getMessage())
      );
    }
  }

  private SingularityDeployResult getDeployResult(
    final SingularityRequest request,
    final RequestState requestState,
    final Optional<SingularityDeployMarker> cancelRequest,
    final SingularityPendingDeploy pendingDeploy,
    final Optional<SingularityUpdatePendingDeployRequest> updatePendingDeployRequest,
    final Optional<SingularityDeploy> maybeDeploy,
    final Collection<SingularityTaskId> deployActiveTasks,
    final Collection<SingularityTaskId> otherActiveTasks,
    final Collection<SingularityTaskId> inactiveDeployMatchingTasks
  ) {
    // Check cancellations first
    if (cancelRequest.isPresent()) {
      LOG.info(
        "Canceling a deploy {} due to cancel request {}",
        pendingDeploy,
        cancelRequest.get()
      );
      String userMessage = cancelRequest.get().getUser().isPresent()
        ? String.format(" by %s", cancelRequest.get().getUser().get())
        : "";
      return new SingularityDeployResult(
        DeployState.CANCELED,
        Optional.of(
          String.format(
            "Canceled due to request%s at %s",
            userMessage,
            cancelRequest.get().getTimestamp()
          )
        ),
        Collections.emptyList(),
        System.currentTimeMillis()
      );
    }

    // Scheduled/on demand/paused things can instantly succeed
    if (
      !request.isDeployable() ||
      (
        configuration.isAllowDeployOfPausedRequests() &&
        requestState == RequestState.PAUSED
      )
    ) {
      LOG.info(
        "Succeeding a deploy {} because the request {} was not deployable",
        pendingDeploy,
        request
      );

      return new SingularityDeployResult(DeployState.SUCCEEDED, "Request not deployable");
    }

    // Shouldn't happen, but possible corrupted state handling
    if (pendingDeploy.getDeployProgress() == null) {
      return new SingularityDeployResult(
        DeployState.FAILED,
        "No deploy progress data present in Zookeeper. Please reattempt your deploy"
      );
    }

    // Check for abandoned pending deploy
    if (!maybeDeploy.isPresent()) {
      LOG.warn("No deploy data found for {}", pendingDeploy.getDeployMarker());
      Optional<SingularityDeployResult> result = deployManager.getDeployResult(
        request.getId(),
        pendingDeploy.getDeployMarker().getDeployId()
      );
      if (result.isPresent() && result.get().getDeployState().isDeployFinished()) {
        LOG.info(
          "Deploy was already finished, running cleanup of pending data for {}",
          pendingDeploy.getDeployMarker()
        );
        return result.get();
      } else {
        return new SingularityDeployResult(
          DeployState.FAILED_INTERNAL_STATE,
          "Deploy data not present"
        );
      }
    }

    SingularityDeploy deploy = maybeDeploy.get();

    // Find tasks that have failed for this deploy (excluding due to framework/cluster issues) since last instance group
    Set<SingularityTaskId> newInactiveDeployTasks = SingularityDeployCheckHelper.getNewInactiveDeployTasks(
      pendingDeploy,
      inactiveDeployMatchingTasks,
      taskManager
    );

    if (!newInactiveDeployTasks.isEmpty()) {
      LOG.info(
        "Found {} inactive tasks for {} - {}",
        newInactiveDeployTasks.size(),
        request.getId(),
        pendingDeploy.getDeployMarker().getDeployId()
      );
      if (CanaryDeployHelper.canRetryTasks(deploy, newInactiveDeployTasks)) {
        updatePendingDeploy(
          pendingDeploy,
          DeployState.WAITING,
          pendingDeploy.getDeployProgress() // Don't update failed tasks list here so it is consistent until the instance group is done
        );
        requestManager.addToPendingQueue(
          new SingularityPendingRequest(
            request.getId(),
            pendingDeploy.getDeployMarker().getDeployId(),
            System.currentTimeMillis(),
            pendingDeploy.getDeployMarker().getUser(),
            PendingType.NEXT_DEPLOY_STEP,
            deploy.getSkipHealthchecksOnDeploy(),
            pendingDeploy.getDeployMarker().getMessage()
          )
        );
        return checkDeployProgress(
          request,
          pendingDeploy,
          updatePendingDeployRequest,
          deploy,
          deployActiveTasks,
          otherActiveTasks,
          inactiveDeployMatchingTasks
        );
      } else {
        // Can't retry, clean up the deploy
        if (
          request.isLoadBalanced() &&
          SingularityDeployCheckHelper.shouldCancelLoadBalancer(pendingDeploy)
        ) {
          LOG.info(
            "Attempting to cancel pending load balancer request, failing deploy {} regardless",
            pendingDeploy
          );
          sendCancelToLoadBalancer(pendingDeploy);
        }

        int maxFailures = deploy
          .getCanaryDeploySettings()
          .getAllowedTasksFailuresPerGroup();
        return deployCheckHelper.getDeployResultWithFailures(
          deploy,
          pendingDeploy,
          DeployState.FAILED,
          String.format(
            "At least %s task(s) for this instance group failed",
            maxFailures
          ),
          inactiveDeployMatchingTasks
        );
      }
    }

    LOG.debug(
      "No inactive tasks found for {} - {}, checking deploy progress",
      request.getId(),
      pendingDeploy.getDeployMarker().getDeployId()
    );
    return checkDeployProgress(
      request,
      pendingDeploy,
      updatePendingDeployRequest,
      deploy,
      deployActiveTasks,
      otherActiveTasks,
      inactiveDeployMatchingTasks
    );
  }

  // We've already checked for tasks that exited with a failure, check status of running things
  private SingularityDeployResult checkDeployProgress(
    final SingularityRequest request,
    final SingularityPendingDeploy pendingDeploy,
    final Optional<SingularityUpdatePendingDeployRequest> updatePendingDeployRequest,
    final SingularityDeploy deploy,
    final Collection<SingularityTaskId> deployActiveTasks,
    final Collection<SingularityTaskId> otherActiveTasks,
    final Collection<SingularityTaskId> inactiveDeployMatchingTasks
  ) {
    SingularityDeployProgress deployProgress = pendingDeploy.getDeployProgress();

    if (deployProgress.isStepLaunchComplete()) {
      // If we are here, checking acceptance conditions before proceeding
      return checkAcceptanceOfDeployStep(
        request,
        deploy,
        pendingDeploy,
        deployActiveTasks,
        updatePendingDeployRequest,
        inactiveDeployMatchingTasks,
        otherActiveTasks
      );
    }

    // LB request was already in progress, check if it's done
    if (SingularityDeployCheckHelper.isWaitingForLbRequest(pendingDeploy)) {
      final SingularityLoadBalancerUpdate lbUpdate = lbClient.getState(
        pendingDeploy
          .getDeployProgress()
          .getPendingLbUpdate()
          .get()
          .getLoadBalancerRequestId()
      );
      return processLbState(
        request,
        deploy,
        pendingDeploy,
        updatePendingDeployRequest,
        deployActiveTasks,
        lbUpdate
      );
    }

    final boolean isDeployOverdue = deployCheckHelper.isDeployOverdue(
      pendingDeploy,
      deploy
    );
    // Cancel any LB updates first if we are overdue to finish
    if (
      isDeployOverdue &&
      request.isLoadBalanced() &&
      SingularityDeployCheckHelper.shouldCancelLoadBalancer(pendingDeploy)
    ) {
      return cancelLoadBalancer(
        pendingDeploy,
        deployCheckHelper.getDeployFailures(
          deploy,
          pendingDeploy,
          DeployState.OVERDUE,
          deployActiveTasks
        )
      );
    }

    // Still waiting for tasks to launch, recheck the pending request + check if we are overdue
    if (deployActiveTasks.size() < deployProgress.getTargetActiveInstances()) {
      maybeUpdatePendingRequest(
        pendingDeploy,
        deploy,
        request,
        updatePendingDeployRequest
      );
      return checkOverdue(deploy, pendingDeploy, deployActiveTasks, isDeployOverdue);
    }

    if (SingularityDeployCheckHelper.isWaitingForLbRequest(pendingDeploy)) {
      // We already checked health last time and triggered the LB, check if done
      return new SingularityDeployResult(
        DeployState.WAITING,
        "Waiting on load balancer API"
      );
    }

    final DeployHealth deployHealth = deployHealthHelper.getDeployHealth(
      request,
      deploy,
      deployActiveTasks,
      true
    );
    switch (deployHealth) {
      case WAITING:
        maybeUpdatePendingRequest(
          pendingDeploy,
          deploy,
          request,
          updatePendingDeployRequest
        );
        return checkOverdue(deploy, pendingDeploy, deployActiveTasks, isDeployOverdue);
      case HEALTHY:
        if (!request.isLoadBalanced()) {
          return markStepLaunchFinished(
            pendingDeploy,
            deploy,
            deployActiveTasks,
            request,
            updatePendingDeployRequest,
            pendingDeploy.getDeployProgress()
          );
        }

        if (
          updatePendingDeployRequest.isPresent() &&
          updatePendingDeployRequest.get().getTargetActiveInstances() !=
          deployProgress.getTargetActiveInstances()
        ) {
          maybeUpdatePendingRequest(
            pendingDeploy,
            deploy,
            request,
            updatePendingDeployRequest
          );
          return new SingularityDeployResult(DeployState.WAITING);
        }

        for (SingularityTaskId activeTaskId : deployActiveTasks) {
          taskManager.markHealthchecksFinished(activeTaskId);
          taskManager.clearStartupHealthchecks(activeTaskId);
        }

        return enqueueAndProcessLbRequest(
          request,
          deploy,
          pendingDeploy,
          updatePendingDeployRequest,
          deployActiveTasks,
          otherActiveTasks
        );
      case UNHEALTHY:
      default:
        for (SingularityTaskId activeTaskId : deployActiveTasks) {
          taskManager.markHealthchecksFinished(activeTaskId);
          taskManager.clearStartupHealthchecks(activeTaskId);
        }
        return deployCheckHelper.getDeployResultWithFailures(
          deploy,
          pendingDeploy,
          DeployState.FAILED,
          "Not all tasks for deploy were healthy",
          deployActiveTasks
        );
    }
  }

  private SingularityDeployResult checkAcceptanceOfDeployStep(
    SingularityRequest request,
    SingularityDeploy deploy,
    SingularityPendingDeploy pendingDeploy,
    Collection<SingularityTaskId> deployActiveTasks,
    Optional<SingularityUpdatePendingDeployRequest> updatePendingDeployRequest,
    Collection<SingularityTaskId> inactiveDeployMatchingTasks,
    Collection<SingularityTaskId> otherActiveTasks
  ) {
    if (updatePendingDeployRequest.isPresent()) {
      maybeUpdatePendingRequest(
        pendingDeploy,
        deploy,
        request,
        updatePendingDeployRequest
      );
      return new SingularityDeployResult(DeployState.WAITING);
    }
    switch (deploy.getCanaryDeploySettings().getAcceptanceMode()) {
      case TIMED:
        if (
          System.currentTimeMillis() >
          pendingDeploy.getDeployProgress().getTimestamp() +
          deploy.getCanaryDeploySettings().getWaitMillisBetweenGroups()
        ) {
          return checkCanaryMaybeFinished(
            request,
            deploy,
            pendingDeploy,
            deployActiveTasks,
            updatePendingDeployRequest,
            inactiveDeployMatchingTasks,
            otherActiveTasks
          );
        } else {
          LOG.info("Waiting for timed deploy step for {}", request.getId());
          return new SingularityDeployResult(DeployState.WAITING);
        }
      case CHECKS:
        Map<String, DeployAcceptanceResult> results = deployAcceptanceManager.getAcceptanceResults(
          request,
          deploy,
          pendingDeploy,
          deployActiveTasks,
          inactiveDeployMatchingTasks,
          otherActiveTasks
        );
        SingularityDeployProgress updatedProgress = pendingDeploy
          .getDeployProgress()
          .withAcceptanceProgress(results);
        DeployState acceptanceHookDeployState = SingularityDeployAcceptanceManager.resultsToDeployState(
          updatedProgress.getStepAcceptanceResults()
        );
        LOG.info("Acceptance checks had result {}", acceptanceHookDeployState);
        if (deploy.getCanaryDeploySettings().isEnableCanaryDeploy()) {
          if (acceptanceHookDeployState == DeployState.SUCCEEDED) {
            if (deployActiveTasks.size() >= request.getInstancesSafe()) {
              cleanupTasks(
                pendingDeploy,
                request,
                DeployState.SUCCEEDED,
                otherActiveTasks
              );
              updatePendingDeploy(
                pendingDeploy,
                acceptanceHookDeployState,
                updatedProgress
              );
              return new SingularityDeployResult(
                acceptanceHookDeployState,
                String.join(", ", updatedProgress.getAcceptanceResultMessageHistory())
              );
            } else {
              return advanceDeployStep(
                updatedProgress,
                request,
                deploy,
                pendingDeploy,
                deployActiveTasks,
                updatePendingDeployRequest,
                inactiveDeployMatchingTasks,
                otherActiveTasks
              );
            }
          } else if (acceptanceHookDeployState == DeployState.FAILED) {
            updatePendingDeploy(
              pendingDeploy,
              acceptanceHookDeployState,
              updatedProgress
            );
            return new SingularityDeployResult(
              acceptanceHookDeployState,
              String.join(", ", updatedProgress.getAcceptanceResultMessageHistory())
            );
          }
          updatePendingDeploy(pendingDeploy, acceptanceHookDeployState, updatedProgress);
          return new SingularityDeployResult(acceptanceHookDeployState);
        } else {
          // Clean up all old tasks on acceptance
          switch (acceptanceHookDeployState) {
            case WAITING:
              updatePendingDeploy(
                pendingDeploy,
                acceptanceHookDeployState,
                updatedProgress
              );
              return new SingularityDeployResult(DeployState.WAITING);
            case SUCCEEDED:
              cleanupTasks(
                pendingDeploy,
                request,
                DeployState.SUCCEEDED,
                otherActiveTasks
              );
              updatePendingDeploy(
                pendingDeploy,
                acceptanceHookDeployState,
                updatedProgress
              );
              return new SingularityDeployResult(DeployState.SUCCEEDED);
            default:
              LOG.info("Acceptance checks failed, cleaning up");
              updatePendingDeploy(
                pendingDeploy,
                acceptanceHookDeployState,
                updatedProgress
              );
              if (request.isLoadBalanced() && lbClient.isEnabled()) {
                // Add previous tasks back to load balancer, since we previously took them out
                if (updatedProgress.getPendingLbUpdate().isPresent()) {
                  return checkLbRevertToActiveTasks(
                    request,
                    deploy,
                    pendingDeploy,
                    updatedProgress,
                    acceptanceHookDeployState,
                    deployActiveTasks,
                    otherActiveTasks
                  );
                } else {
                  return enqueueLbRevertToActiveTasks(
                    request,
                    pendingDeploy,
                    updatedProgress,
                    deployActiveTasks,
                    otherActiveTasks
                  );
                }
              } else {
                return new SingularityDeployResult(
                  acceptanceHookDeployState,
                  String.join(", ", updatedProgress.getAcceptanceResultMessageHistory())
                );
              }
          }
        }
      case NONE:
      default:
        return checkCanaryMaybeFinished(
          request,
          deploy,
          pendingDeploy,
          deployActiveTasks,
          updatePendingDeployRequest,
          inactiveDeployMatchingTasks,
          otherActiveTasks
        );
    }
  }

  private SingularityDeployResult checkLbRevertToActiveTasks(
    SingularityRequest request,
    SingularityDeploy deploy,
    SingularityPendingDeploy pendingDeploy,
    SingularityDeployProgress updatedProgress,
    DeployState acceptanceHookDeployState,
    Collection<SingularityTaskId> deployActiveTasks,
    Collection<SingularityTaskId> otherActiveTasks
  ) {
    final SingularityLoadBalancerUpdate lbUpdate = lbClient.getState(
      pendingDeploy
        .getDeployProgress()
        .getPendingLbUpdate()
        .get()
        .getLoadBalancerRequestId()
    );
    DeployProgressLbUpdateHolder lbUpdateHolder = updatedProgress
      .getLbUpdates()
      .get(lbUpdate.getLoadBalancerRequestId().toString());
    if (lbUpdateHolder == null) {
      return new SingularityDeployResult(
        DeployState.FAILED_INTERNAL_STATE,
        "Load balancer update metadata not found"
      );
    }
    updateLoadBalancerStateForTasks(
      lbUpdateHolder.getAdded(),
      LoadBalancerRequestType.ADD,
      lbUpdate
    );
    switch (lbUpdate.getLoadBalancerState()) {
      case SUCCESS:
        LOG.info("LB revert succeeded, continuing with original deploy status");
        updatePendingDeploy(
          pendingDeploy,
          acceptanceHookDeployState,
          updatedProgress.withFinishedLbUpdate(lbUpdate, lbUpdateHolder)
        );
        return new SingularityDeployResult(
          acceptanceHookDeployState,
          String.join(", ", updatedProgress.getAcceptanceResultMessageHistory())
        );
      case WAITING:
        return new SingularityDeployResult(DeployState.WAITING);
      default:
        // Keep trying until we time out, since abandoning here would leave nothing in the LB
        if (deployCheckHelper.isDeployOverdue(pendingDeploy, deploy)) {
          LOG.error("Unable to revert load balancer for deploy failure");
          return new SingularityDeployResult(
            DeployState.FAILED_INTERNAL_STATE,
            "Unable to revert load balancer for deploy failure"
          );
        } else {
          LOG.warn(
            "Retrying failed LB revert for {} {}",
            pendingDeploy.getDeployMarker().getRequestId(),
            pendingDeploy.getDeployMarker().getDeployId()
          );
          return enqueueLbRevertToActiveTasks(
            request,
            pendingDeploy,
            updatedProgress,
            deployActiveTasks,
            otherActiveTasks
          );
        }
    }
  }

  private SingularityDeployResult enqueueLbRevertToActiveTasks(
    SingularityRequest request,
    SingularityPendingDeploy pendingDeploy,
    SingularityDeployProgress updatedProgress,
    Collection<SingularityTaskId> deployActiveTasks,
    Collection<SingularityTaskId> otherActiveTasks
  ) {
    Optional<String> activeDeployIdToRevertTo = deployManager.getActiveDeployId(
      request.getId()
    );
    if (!activeDeployIdToRevertTo.isPresent()) {
      return new SingularityDeployResult(
        DeployState.FAILED_INTERNAL_STATE,
        "No active deploy id  found to revert to"
      );
    }
    Optional<SingularityDeploy> activeDeployToRevertTo = deployManager.getDeploy(
      request.getId(),
      activeDeployIdToRevertTo.get()
    );
    if (!activeDeployToRevertTo.isPresent()) {
      return new SingularityDeployResult(
        DeployState.FAILED_INTERNAL_STATE,
        "No active deploy found to revert to"
      );
    }
    SingularityDeploy toRevertTo = activeDeployToRevertTo.get();
    Set<SingularityTaskId> toAddBack = otherActiveTasks
      .stream()
      .filter(t -> t.getDeployId().equals(toRevertTo.getId()))
      .collect(Collectors.toSet());

    LOG.info("Attempting to roll back load balancer to previous active tasks");
    // If we have gotten here, there is both an add and remove in the LB history and task is _not_ in the LB
    // Many steps are based on the presence of an add when determining actions to take on shut down. So, clear
    // this to make it as if the task was a net-new add to the LB, making cleanup act as before
    for (SingularityTaskId toAddId : toAddBack) {
      taskManager.clearLoadBalancerHistory(toAddId);
    }
    Map<SingularityTaskId, SingularityTask> tasks = taskManager.getTasks(
      Iterables.concat(deployActiveTasks, toAddBack)
    );
    final LoadBalancerRequestId lbRequestId = SingularityDeployCheckHelper.getRequestIdForRevert(
      request.getId(),
      toRevertTo.getId()
    );
    SingularityLoadBalancerUpdate enqueueResult = lbClient.enqueue(
      lbRequestId,
      request,
      toRevertTo,
      SingularityDeployCheckHelper.getTasks(toAddBack, tasks),
      SingularityDeployCheckHelper.getTasks(deployActiveTasks, tasks)
    );
    SingularityDeployProgress deployProgress = updatedProgress.withPendingLbUpdate(
      enqueueResult,
      toAddBack,
      deployActiveTasks,
      true
    );
    updatePendingDeploy(pendingDeploy, DeployState.WAITING, deployProgress);
    return new SingularityDeployResult(DeployState.WAITING);
  }

  private SingularityDeployResult checkCanaryMaybeFinished(
    SingularityRequest request,
    SingularityDeploy deploy,
    SingularityPendingDeploy pendingDeploy,
    Collection<SingularityTaskId> deployActiveTasks,
    Optional<SingularityUpdatePendingDeployRequest> updatePendingDeployRequest,
    Collection<SingularityTaskId> inactiveDeployMatchingTasks,
    Collection<SingularityTaskId> otherActiveTasks
  ) {
    if (deploy.getCanaryDeploySettings().isEnableCanaryDeploy()) {
      if (deployActiveTasks.size() >= request.getInstancesSafe()) {
        cleanupTasks(pendingDeploy, request, DeployState.SUCCEEDED, otherActiveTasks);
        updatePendingDeploy(
          pendingDeploy,
          DeployState.SUCCEEDED,
          pendingDeploy.getDeployProgress()
        );
        return new SingularityDeployResult(DeployState.SUCCEEDED);
      } else {
        return advanceDeployStep(
          pendingDeploy.getDeployProgress(),
          request,
          deploy,
          pendingDeploy,
          deployActiveTasks,
          updatePendingDeployRequest,
          inactiveDeployMatchingTasks,
          otherActiveTasks
        );
      }
    } else {
      return new SingularityDeployResult(DeployState.SUCCEEDED);
    }
  }

  private SingularityDeployResult advanceDeployStep(
    SingularityDeployProgress deployProgressCurrent,
    SingularityRequest request,
    SingularityDeploy deploy,
    SingularityPendingDeploy pendingDeploy,
    Collection<SingularityTaskId> deployActiveTasks,
    Optional<SingularityUpdatePendingDeployRequest> updatePendingDeployRequest,
    Collection<SingularityTaskId> inactiveDeployMatchingTasks,
    Collection<SingularityTaskId> otherActiveTasks
  ) {
    LOG.info(
      "Deploy {} has completed step to {} instances (out of {})",
      pendingDeploy.getDeployMarker(),
      pendingDeploy.getDeployProgress().getTargetActiveInstances(),
      request.getInstancesSafe()
    );
    SingularityDeployProgress newStepProgress = deployProgressCurrent
      .withNewTargetInstances(
        CanaryDeployHelper.getNewTargetInstances(
          deployProgressCurrent,
          request,
          updatePendingDeployRequest,
          deploy.getCanaryDeploySettings()
        ),
        deployActiveTasks.size(),
        true
      )
      // Keep the list of previous failed task ids so they can be excluded from next groups check
      .withFailedTasks(new HashSet<>(inactiveDeployMatchingTasks));
    cleanupTasks(
      pendingDeploy,
      request,
      DeployState.SUCCEEDED,
      CanaryDeployHelper.tasksToShutDown(deployProgressCurrent, otherActiveTasks, request)
    );
    requestManager.addToPendingQueue(
      new SingularityPendingRequest(
        request.getId(),
        pendingDeploy.getDeployMarker().getDeployId(),
        System.currentTimeMillis(),
        pendingDeploy.getDeployMarker().getUser(),
        PendingType.NEXT_DEPLOY_STEP,
        deploy.getSkipHealthchecksOnDeploy(),
        pendingDeploy.getDeployMarker().getMessage()
      )
    );
    updatePendingDeploy(pendingDeploy, DeployState.WAITING, newStepProgress);
    return new SingularityDeployResult(DeployState.WAITING);
  }

  private SingularityDeployResult enqueueAndProcessLbRequest(
    SingularityRequest request,
    SingularityDeploy deploy,
    SingularityPendingDeploy pendingDeploy,
    Optional<SingularityUpdatePendingDeployRequest> updatePendingDeployRequest,
    Collection<SingularityTaskId> deployActiveTasks,
    Collection<SingularityTaskId> otherActiveTasks
  ) {
    Collection<SingularityTaskId> toShutDown;
    if (deploy.getCanaryDeploySettings().isEnableCanaryDeploy()) {
      // Only add new instances, old will be removed after acceptance conditions are met
      toShutDown = Collections.emptySet();
    } else {
      // Remove all old, add all new, in one lb update
      toShutDown = otherActiveTasks;
    }

    final Map<SingularityTaskId, SingularityTask> tasks = taskManager.getTasks(
      Iterables.concat(deployActiveTasks, toShutDown)
    );
    final LoadBalancerRequestId lbRequestId = SingularityDeployCheckHelper.getNewLoadBalancerRequestId(
      pendingDeploy
    );

    List<SingularityTaskId> toRemoveFromLb = new ArrayList<>();
    for (SingularityTaskId taskId : toShutDown) {
      Optional<SingularityLoadBalancerUpdate> maybeAddUpdate = taskManager.getLoadBalancerState(
        taskId,
        LoadBalancerRequestType.ADD
      );
      if (
        maybeAddUpdate.isPresent() &&
        (
          maybeAddUpdate.get().getLoadBalancerState() ==
          LoadBalancerRequestState.SUCCESS ||
          maybeAddUpdate.get().getLoadBalancerState().isInProgress()
        )
      ) {
        toRemoveFromLb.add(taskId);
      }
    }

    updateLoadBalancerStateForTasks(
      deployActiveTasks,
      LoadBalancerRequestType.ADD,
      SingularityLoadBalancerUpdate.preEnqueue(lbRequestId)
    );
    updateLoadBalancerStateForTasks(
      toRemoveFromLb,
      LoadBalancerRequestType.REMOVE,
      SingularityLoadBalancerUpdate.preEnqueue(lbRequestId)
    );
    LOG.debug(
      "Updating load balancer. Adding: {}, Removing: {}",
      deployActiveTasks,
      toRemoveFromLb
    );
    SingularityLoadBalancerUpdate enqueueResult = lbClient.enqueue(
      lbRequestId,
      request,
      deploy,
      SingularityDeployCheckHelper.getTasks(deployActiveTasks, tasks),
      SingularityDeployCheckHelper.getTasks(toShutDown, tasks)
    );
    // Save the lb enqueue + added/removed tasks for later
    SingularityDeployProgress deployProgress = pendingDeploy
      .getDeployProgress()
      .withPendingLbUpdate(enqueueResult, deployActiveTasks, toRemoveFromLb, false);
    updatePendingDeploy(pendingDeploy, DeployState.WAITING, deployProgress);
    maybeUpdatePendingRequest(pendingDeploy, deploy, request, updatePendingDeployRequest);
    return new SingularityDeployResult(DeployState.WAITING);
  }

  private SingularityDeployResult processLbState(
    SingularityRequest request,
    SingularityDeploy deploy,
    SingularityPendingDeploy pendingDeploy,
    Optional<SingularityUpdatePendingDeployRequest> updatePendingDeployRequest,
    Collection<SingularityTaskId> deployActiveTasks,
    SingularityLoadBalancerUpdate lbUpdate
  ) {
    SingularityDeployProgress deployProgress = pendingDeploy.getDeployProgress();
    DeployProgressLbUpdateHolder lbUpdateHolder = deployProgress
      .getLbUpdates()
      .get(lbUpdate.getLoadBalancerRequestId().toString());
    if (lbUpdateHolder == null) {
      return new SingularityDeployResult(
        DeployState.FAILED_INTERNAL_STATE,
        "Load balancer update metadata not found"
      );
    }

    updateLoadBalancerStateForTasks(
      lbUpdateHolder.getAdded(),
      LoadBalancerRequestType.ADD,
      lbUpdate
    );
    updateLoadBalancerStateForTasks(
      lbUpdateHolder.getRemoved(),
      LoadBalancerRequestType.REMOVE,
      lbUpdate
    );

    DeployState deployState = SingularityDeployCheckHelper.interpretLoadBalancerState(
      lbUpdate,
      pendingDeploy.getCurrentDeployState()
    );
    if (deployState == DeployState.SUCCEEDED) {
      SingularityDeployProgress updatedProgress = deployProgress.withFinishedLbUpdate(
        lbUpdate,
        lbUpdateHolder
      );
      updatePendingDeploy(pendingDeploy, DeployState.WAITING, updatedProgress);
      // All tasks for current step are launched and in the LB if needed
      return markStepLaunchFinished(
        pendingDeploy,
        deploy,
        deployActiveTasks,
        request,
        updatePendingDeployRequest,
        updatedProgress
      );
    } else if (deployState == DeployState.WAITING) {
      updatePendingDeploy(
        pendingDeploy,
        deployState,
        deployProgress.withPendingLbUpdate(
          lbUpdate,
          lbUpdateHolder.getAdded(),
          lbUpdateHolder.getRemoved(),
          false
        )
      );
      maybeUpdatePendingRequest(
        pendingDeploy,
        deploy,
        request,
        updatePendingDeployRequest
      );
      return new SingularityDeployResult(DeployState.WAITING);
    } else {
      updatePendingDeploy(
        pendingDeploy,
        deployState,
        deployProgress.withFinishedLbUpdate(lbUpdate, lbUpdateHolder)
      );
      maybeUpdatePendingRequest(
        pendingDeploy,
        deploy,
        request,
        updatePendingDeployRequest
      );
      return new SingularityDeployResult(
        deployState,
        lbUpdate,
        SingularityDeployFailure.lbUpdateFailed()
      );
    }
  }

  private void maybeUpdatePendingRequest(
    SingularityPendingDeploy pendingDeploy,
    SingularityDeploy deploy,
    SingularityRequest request,
    Optional<SingularityUpdatePendingDeployRequest> updatePendingDeployRequest
  ) {
    if (updatePendingDeployRequest.isPresent()) {
      SingularityDeployProgress newProgress = pendingDeploy
        .getDeployProgress()
        .withNewTargetInstances(
          Math.min(
            updatePendingDeployRequest.get().getTargetActiveInstances(),
            request.getInstancesSafe()
          ),
          pendingDeploy.getDeployProgress().getCurrentActiveInstances(),
          true
        );
      updatePendingDeploy(pendingDeploy, DeployState.WAITING, newProgress);
      requestManager.addToPendingQueue(
        new SingularityPendingRequest(
          request.getId(),
          pendingDeploy.getDeployMarker().getDeployId(),
          System.currentTimeMillis(),
          pendingDeploy.getDeployMarker().getUser(),
          PendingType.NEXT_DEPLOY_STEP,
          deploy.getSkipHealthchecksOnDeploy(),
          pendingDeploy.getDeployMarker().getMessage()
        )
      );
    }
  }

  private SingularityDeployResult markStepLaunchFinished(
    SingularityPendingDeploy pendingDeploy,
    SingularityDeploy deploy,
    Collection<SingularityTaskId> deployActiveTasks,
    SingularityRequest request,
    Optional<SingularityUpdatePendingDeployRequest> updatePendingDeployRequest,
    SingularityDeployProgress deployProgress
  ) {
    if (updatePendingDeployRequest.isPresent()) {
      maybeUpdatePendingRequest(
        pendingDeploy,
        deploy,
        request,
        updatePendingDeployRequest
      );
      return new SingularityDeployResult(DeployState.WAITING);
    } else {
      // All tasks launched + LB updates done, next cycle will check for acceptance conditions
      SingularityDeployProgress newProgress = deployProgress
        .withNewActiveInstances(deployActiveTasks.size())
        .withCompletedStepLaunch();
      updatePendingDeploy(pendingDeploy, DeployState.WAITING, newProgress);
      return new SingularityDeployResult(DeployState.WAITING);
    }
  }

  private SingularityDeployResult checkOverdue(
    SingularityDeploy deploy,
    SingularityPendingDeploy pendingDeploy,
    Collection<SingularityTaskId> deployActiveTasks,
    boolean isOverdue
  ) {
    String message = String.format(
      "Deploy was able to launch %s tasks, but not all of them became healthy within %s",
      deployActiveTasks.size(),
      JavaUtils.durationFromMillis(deployCheckHelper.getAllowedMillis(deploy))
    );

    if (isOverdue) {
      return deployCheckHelper.getDeployResultWithFailures(
        deploy,
        pendingDeploy,
        DeployState.OVERDUE,
        message,
        deployActiveTasks
      );
    } else {
      return new SingularityDeployResult(DeployState.WAITING);
    }
  }
}
