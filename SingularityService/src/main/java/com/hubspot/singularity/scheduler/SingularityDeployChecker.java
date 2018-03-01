package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.api.common.LoadBalancerRequestType;
import com.hubspot.singularity.api.common.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.api.common.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.api.deploy.DeployState;
import com.hubspot.singularity.api.deploy.SingularityDeploy;
import com.hubspot.singularity.api.deploy.SingularityDeployFailure;
import com.hubspot.singularity.api.deploy.SingularityDeployFailureReason;
import com.hubspot.singularity.api.deploy.SingularityDeployKey;
import com.hubspot.singularity.api.deploy.SingularityDeployMarker;
import com.hubspot.singularity.api.deploy.SingularityDeployProgress;
import com.hubspot.singularity.api.deploy.SingularityDeployResult;
import com.hubspot.singularity.api.expiring.SingularityExpiringPause;
import com.hubspot.singularity.api.expiring.SingularityExpiringScale;
import com.hubspot.singularity.api.request.RequestState;
import com.hubspot.singularity.api.request.RequestType;
import com.hubspot.singularity.api.request.SingularityPendingDeploy;
import com.hubspot.singularity.api.request.SingularityPendingRequest;
import com.hubspot.singularity.api.request.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.api.request.SingularityPendingRequestBuilder;
import com.hubspot.singularity.api.request.SingularityRequest;
import com.hubspot.singularity.api.request.SingularityRequestDeployState;
import com.hubspot.singularity.api.request.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.api.request.SingularityRequestWithState;
import com.hubspot.singularity.api.request.SingularityRunNowRequest;
import com.hubspot.singularity.api.request.SingularityUpdatePendingDeployRequest;
import com.hubspot.singularity.api.task.SingularityPendingTaskId;
import com.hubspot.singularity.api.task.SingularityTask;
import com.hubspot.singularity.api.task.SingularityTaskCleanup;
import com.hubspot.singularity.api.task.SingularityTaskId;
import com.hubspot.singularity.api.task.TaskCleanupType;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.hooks.LoadBalancerClient;
import com.hubspot.singularity.mesos.SingularitySchedulerLock;
import com.hubspot.singularity.scheduler.SingularityDeployHealthHelper.DeployHealth;

@Singleton
public class SingularityDeployChecker {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityDeployChecker.class);

  private final DeployManager deployManager;
  private final TaskManager taskManager;
  private final SingularityDeployHealthHelper deployHealthHelper;
  private final RequestManager requestManager;
  private final SingularityConfiguration configuration;
  private final LoadBalancerClient lbClient;
  private final SingularitySchedulerLock lock;

  private final ExecutorService deployCheckerExecutor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("deploy-checker-%d").build());

  @Inject
  public SingularityDeployChecker(DeployManager deployManager, SingularityDeployHealthHelper deployHealthHelper, LoadBalancerClient lbClient, RequestManager requestManager, TaskManager taskManager,
                                  SingularityConfiguration configuration, SingularitySchedulerLock lock) {
    this.configuration = configuration;
    this.lbClient = lbClient;
    this.deployHealthHelper = deployHealthHelper;
    this.requestManager = requestManager;
    this.deployManager = deployManager;
    this.taskManager = taskManager;
    this.lock = lock;
  }

  public int checkDeploys() {
    final List<SingularityPendingDeploy> pendingDeploys = deployManager.getPendingDeploys();
    final List<SingularityDeployMarker> cancelDeploys = deployManager.getCancelDeploys();
    final List<SingularityUpdatePendingDeployRequest> updateRequests = deployManager.getPendingDeployUpdates();

    if (pendingDeploys.isEmpty() && cancelDeploys.isEmpty()) {
      return 0;
    }

    final Map<SingularityPendingDeploy, SingularityDeployKey> pendingDeployToKey = SingularityDeployKey.fromPendingDeploys(pendingDeploys);
    final Map<SingularityDeployKey, SingularityDeploy> deployKeyToDeploy = deployManager.getDeploysForKeys(pendingDeployToKey.values());

    pendingDeploys.parallelStream().forEach((pendingDeploy) -> {
      lock.runWithRequestLock(() -> {
        checkDeploy(pendingDeploy, cancelDeploys, pendingDeployToKey, deployKeyToDeploy, updateRequests);
      }, pendingDeploy.getDeployMarker().getRequestId(), getClass().getSimpleName());
    });

    cancelDeploys.forEach(deployManager::deleteCancelDeployRequest);
    updateRequests.forEach(deployManager::deleteUpdatePendingDeployRequest);

    return pendingDeploys.size();
  }

  private void checkDeploy(final SingularityPendingDeploy pendingDeploy, final List<SingularityDeployMarker> cancelDeploys,
    final Map<SingularityPendingDeploy, SingularityDeployKey> pendingDeployToKey, final Map<SingularityDeployKey, SingularityDeploy> deployKeyToDeploy,
    List<SingularityUpdatePendingDeployRequest> updateRequests) {
    final SingularityDeployKey deployKey = pendingDeployToKey.get(pendingDeploy);
    final Optional<SingularityDeploy> deploy = Optional.ofNullable(deployKeyToDeploy.get(deployKey));

    Optional<SingularityRequestWithState> maybeRequestWithState = requestManager.getRequest(pendingDeploy.getDeployMarker().getRequestId());

    if (!(maybeRequestWithState.isPresent() && maybeRequestWithState.get().getState() == RequestState.FINISHED)
        && !SingularityRequestWithState.isActive(maybeRequestWithState)) {
      LOG.warn("Deploy {} request was {}, removing deploy", pendingDeploy, SingularityRequestWithState.getRequestState(maybeRequestWithState));

      if (shouldCancelLoadBalancer(pendingDeploy)) {
        cancelLoadBalancer(pendingDeploy, SingularityDeployFailure.deployRemoved());
      }

      failPendingDeployDueToState(pendingDeploy, maybeRequestWithState, deploy);
      return;
    }

    final SingularityDeployMarker pendingDeployMarker = pendingDeploy.getDeployMarker();

    final Optional<SingularityDeployMarker> cancelRequest = findCancel(cancelDeploys, pendingDeployMarker);
    final Optional<SingularityUpdatePendingDeployRequest> updatePendingDeployRequest = findUpdateRequest(updateRequests, pendingDeploy);

    final SingularityRequestWithState requestWithState = maybeRequestWithState.get();
    final SingularityRequest request = pendingDeploy.getUpdatedRequest().orElse(requestWithState.getRequest());

    final List<SingularityTaskId> requestTasks = taskManager.getTaskIdsForRequest(request.getId());
    final List<SingularityTaskId> activeTasks = taskManager.filterActiveTaskIds(requestTasks);

    final List<SingularityTaskId> inactiveDeployMatchingTasks = new ArrayList<>(requestTasks.size());

    for (SingularityTaskId taskId : requestTasks) {
      if (taskId.getDeployId().equals(pendingDeployMarker.getDeployId()) && !activeTasks.contains(taskId)) {
        inactiveDeployMatchingTasks.add(taskId);
      }
    }

    final List<SingularityTaskId> deployMatchingTasks = new ArrayList<>(activeTasks.size());
    final List<SingularityTaskId> allOtherMatchingTasks = new ArrayList<>(activeTasks.size());

    for (SingularityTaskId taskId : activeTasks) {
      if (taskId.getDeployId().equals(pendingDeployMarker.getDeployId())) {
        deployMatchingTasks.add(taskId);
      } else {
        allOtherMatchingTasks.add(taskId);
      }
    }

    SingularityDeployResult deployResult =
      getDeployResult(request, cancelRequest, pendingDeploy, updatePendingDeployRequest, deploy, deployMatchingTasks, allOtherMatchingTasks, inactiveDeployMatchingTasks);

    LOG.info("Deploy {} had result {} after {}", pendingDeployMarker, deployResult, JavaUtils.durationFromMillis(System.currentTimeMillis() - pendingDeployMarker.getTimestamp()));

    if (deployResult.getDeployState() == DeployState.SUCCEEDED) {
      if (saveNewDeployState(pendingDeployMarker, Optional.of(pendingDeployMarker))) {
        if (!(request.getRequestType() == RequestType.RUN_ONCE)) {
          deleteObsoletePendingTasks(pendingDeploy);
        }
        finishDeploy(requestWithState, deploy, pendingDeploy, allOtherMatchingTasks, deployResult);
        return;
      } else {
        LOG.warn("Failing deploy {} because it failed to save deploy state", pendingDeployMarker);
        deployResult =
          new SingularityDeployResult(DeployState.FAILED_INTERNAL_STATE, Optional.of(String.format("Deploy had state %s but failed to persist it correctly", deployResult.getDeployState())),
            deployResult.getLbUpdate(), SingularityDeployFailure.failedToSave(), deployResult.getTimestamp());
      }
    } else if (!deployResult.getDeployState().isDeployFinished()) {
      return;
    }

    // success case is handled, handle failure cases:
    saveNewDeployState(pendingDeployMarker, Optional.empty());
    finishDeploy(requestWithState, deploy, pendingDeploy, deployMatchingTasks, deployResult);
  }

  private void deleteObsoletePendingTasks(SingularityPendingDeploy pendingDeploy) {
    List<SingularityPendingTaskId> obsoletePendingTasks = taskManager.getPendingTaskIds()
        .stream()
        .filter(taskId -> taskId.getRequestId().equals(pendingDeploy.getDeployMarker().getRequestId()))
        .filter(taskId -> !taskId.getDeployId().equals(pendingDeploy.getDeployMarker().getDeployId()))
        .collect(Collectors.toList());

    for (SingularityPendingTaskId pendingTaskId : obsoletePendingTasks) {
      LOG.debug("Deleting obsolete pending task {}", pendingTaskId.getId());
      taskManager.deletePendingTask(pendingTaskId);
    }
  }

  private Optional<SingularityDeployMarker> findCancel(List<SingularityDeployMarker> cancelDeploys, SingularityDeployMarker activeDeploy) {
    for (SingularityDeployMarker cancelDeploy : cancelDeploys) {
      if (cancelDeploy.getRequestId().equals(activeDeploy.getRequestId()) && cancelDeploy.getDeployId().equals(activeDeploy.getDeployId())) {
        return Optional.of(cancelDeploy);
      }
    }

    return Optional.empty();
  }

  private Optional<SingularityUpdatePendingDeployRequest> findUpdateRequest(List<SingularityUpdatePendingDeployRequest> updateRequests, SingularityPendingDeploy pendingDeploy) {
    for (SingularityUpdatePendingDeployRequest updateRequest : updateRequests) {
      if (updateRequest.getRequestId().equals(pendingDeploy.getDeployMarker().getRequestId()) && updateRequest.getDeployId().equals(pendingDeploy.getDeployMarker().getDeployId())) {
        return Optional.of(updateRequest);
      }
    }
    return Optional.empty();
  }

  private void updateLoadBalancerStateForTasks(Collection<SingularityTaskId> taskIds, LoadBalancerRequestType type, SingularityLoadBalancerUpdate update) {
    for (SingularityTaskId taskId : taskIds) {
      taskManager.saveLoadBalancerState(taskId, type, update);
    }
  }

  private void cleanupTasks(SingularityPendingDeploy pendingDeploy, SingularityRequest request, SingularityDeployResult deployResult, Iterable<SingularityTaskId> tasksToKill) {
    for (SingularityTaskId matchingTask : tasksToKill) {
      taskManager.saveTaskCleanup(new SingularityTaskCleanup(pendingDeploy.getDeployMarker().getUser(), getCleanupType(pendingDeploy, request, deployResult), deployResult.getTimestamp(), matchingTask,
        Optional.of(String.format("Deploy %s - %s", pendingDeploy.getDeployMarker().getDeployId(), deployResult.getDeployState().name())), Optional.empty(), Optional.empty()));
    }
  }

  private TaskCleanupType getCleanupType(SingularityPendingDeploy pendingDeploy, SingularityRequest request, SingularityDeployResult deployResult) {
    if (pendingDeploy.getDeployProgress().isPresent() && pendingDeploy.getDeployProgress().get().getDeployInstanceCountPerStep() != request.getInstancesSafe()) {
      // For incremental deploys, return a special cleanup type
      if (deployResult.getDeployState() == DeployState.FAILED) {
        return TaskCleanupType.INCREMENTAL_DEPLOY_FAILED;
      } else if (deployResult.getDeployState() == DeployState.CANCELED) {
        return TaskCleanupType.INCREMENTAL_DEPLOY_CANCELLED;
      }
    }
    return deployResult.getDeployState().getCleanupType();
  }

  private boolean saveNewDeployState(SingularityDeployMarker pendingDeployMarker, Optional<SingularityDeployMarker> newActiveDeploy) {
    Optional<SingularityRequestDeployState> deployState = deployManager.getRequestDeployState(pendingDeployMarker.getRequestId());

    if (!deployState.isPresent()) {
      LOG.error("Expected deploy state for deploy marker: {} but didn't find it", pendingDeployMarker);
      return false;
    }

    deployManager.saveNewRequestDeployState(new SingularityRequestDeployState(deployState.get().getRequestId(), newActiveDeploy.isPresent() ? newActiveDeploy : deployState.get().getActiveDeploy(), Optional.empty()));

    return true;
  }

  private void finishDeploy(SingularityRequestWithState requestWithState, Optional<SingularityDeploy> deploy, SingularityPendingDeploy pendingDeploy, Iterable<SingularityTaskId> tasksToKill,
    SingularityDeployResult deployResult) {
    SingularityRequest request = requestWithState.getRequest();

    if (!request.isOneOff() && !(request.getRequestType() == RequestType.RUN_ONCE)) {
      cleanupTasks(pendingDeploy, request, deployResult, tasksToKill);
    }

    if (deploy.isPresent() && deploy.get().getRunImmediately().isPresent()) {
      String requestId = deploy.get().getRequestId();
      String deployId = deploy.get().getId();
      SingularityRunNowRequest runNowRequest = deploy.get().getRunImmediately().get();
      List<SingularityTaskId> activeTasks = taskManager.getActiveTaskIdsForRequest(requestId);
      List<SingularityPendingTaskId> pendingTasks = taskManager.getPendingTaskIdsForRequest(requestId);

      SingularityPendingRequestBuilder builder = new SingularityPendingRequestBuilder()
          .setRequestId(requestId)
          .setDeployId(deployId)
          .setTimestamp(deployResult.getTimestamp())
          .setUser(pendingDeploy.getDeployMarker().getUser())
          .setCmdLineArgsList(runNowRequest.getCommandLineArgs())
          .setRunId(runNowRequest.getRunId().isPresent() ? runNowRequest.getRunId() : Optional.of(UUID.randomUUID().toString()))
          .setSkipHealthchecks(runNowRequest.getSkipHealthchecks().isPresent() ? runNowRequest.getSkipHealthchecks() : request.getSkipHealthchecks())
          .setMessage(runNowRequest.getMessage().isPresent() ? runNowRequest.getMessage() : pendingDeploy.getDeployMarker().getMessage())
          .setResources(runNowRequest.getResources())
          .setRunAsUserOverride(runNowRequest.getRunAsUserOverride())
          .setEnvOverrides(runNowRequest.getEnvOverrides())
          .setExtraArtifacts(runNowRequest.getExtraArtifacts())
          .setRunAt(runNowRequest.getRunAt());

      PendingType pendingType = null;
      if (request.isScheduled()) {
        if (activeTasks.isEmpty()) {
          pendingType = PendingType.IMMEDIATE;
        } else {
          // Don't run scheduled task over a running task. Will be picked up on the next run.
          pendingType = PendingType.NEW_DEPLOY;
        }
      } else if (!request.isLongRunning()) {
        if (request.getInstances().isPresent()
            && (activeTasks.size() + pendingTasks.size() < request.getInstances().get())) {
          pendingType = PendingType.ONEOFF;
        } else {
          // Don't run one-off / on-demand task when already at instance count cap
          pendingType = PendingType.NEW_DEPLOY;
        }
      }
      if (pendingType != null) {
        builder.setPendingType(canceledOr(deployResult.getDeployState(), pendingType));
        requestManager.addToPendingQueue(builder.build());
      } else {
        LOG.warn("Could not determine pending type for deploy {}.", deployId);
      }

    } else if (!request.isDeployable() && !request.isOneOff()) {
      PendingType pendingType = canceledOr(deployResult.getDeployState(), PendingType.NEW_DEPLOY);
      requestManager.addToPendingQueue(new SingularityPendingRequest(request.getId(), pendingDeploy.getDeployMarker().getDeployId(),
          deployResult.getTimestamp(), pendingDeploy.getDeployMarker().getUser(), pendingType,
          deploy.isPresent() ? deploy.get().getSkipHealthchecksOnDeploy() : Optional.empty(), pendingDeploy.getDeployMarker().getMessage()));
    }

    if (deployResult.getDeployState() == DeployState.SUCCEEDED) {
      if (!request.isDeployable() && !request.isOneOff()) {
        // remove the lock on bounces in case we deployed during a bounce
        requestManager.markBounceComplete(request.getId());
      }
      if (requestWithState.getState() == RequestState.FINISHED) {
        // A FINISHED request is moved to ACTIVE state so we can reevaluate the schedule
        requestManager.activate(
            request,
            RequestHistoryType.UPDATED,
            System.currentTimeMillis(),
            deploy.isPresent() ? deploy.get().getUser() : Optional.empty(),
            Optional.empty());
      }
    }

    deployManager.saveDeployResult(pendingDeploy.getDeployMarker(), deploy, deployResult);

    if (request.isDeployable() && (deployResult.getDeployState() == DeployState.CANCELED || deployResult.getDeployState() == DeployState.FAILED || deployResult.getDeployState() == DeployState.OVERDUE)) {
      Optional<SingularityRequestDeployState> maybeRequestDeployState = deployManager.getRequestDeployState(request.getId());
      if (maybeRequestDeployState.isPresent()
        && maybeRequestDeployState.get().getActiveDeploy().isPresent()
        && !(requestWithState.getState() == RequestState.PAUSED || requestWithState.getState() == RequestState.DEPLOYING_TO_UNPAUSE)) {
        requestManager.addToPendingQueue(new SingularityPendingRequest(
          request.getId(),
          maybeRequestDeployState.get().getActiveDeploy().get().getDeployId(),
          deployResult.getTimestamp(),
          pendingDeploy.getDeployMarker().getUser(),
          deployResult.getDeployState() == DeployState.CANCELED ? PendingType.DEPLOY_CANCELLED : PendingType.DEPLOY_FAILED,
          request.getSkipHealthchecks(),
          pendingDeploy.getDeployMarker().getMessage()));
      }
    }

    if (request.isDeployable() && deployResult.getDeployState() == DeployState.SUCCEEDED && pendingDeploy.getDeployProgress().isPresent()) {
      if (pendingDeploy.getDeployProgress().get().getTargetActiveInstances() != request.getInstancesSafe()) {
        requestManager.addToPendingQueue(new SingularityPendingRequest(request.getId(), pendingDeploy.getDeployMarker().getDeployId(), deployResult.getTimestamp(),
          pendingDeploy.getDeployMarker().getUser(), PendingType.UPDATED_REQUEST, request.getSkipHealthchecks(), pendingDeploy.getDeployMarker().getMessage()));
      }
    }

    if (requestWithState.getState() == RequestState.DEPLOYING_TO_UNPAUSE) {
      if (deployResult.getDeployState() == DeployState.SUCCEEDED) {
        requestManager.activate(request, RequestHistoryType.DEPLOYED_TO_UNPAUSE, deployResult.getTimestamp(), pendingDeploy.getDeployMarker().getUser(), Optional.empty());
        requestManager.deleteExpiringObject(SingularityExpiringPause.class, request.getId());
      } else {
        requestManager.pause(request, deployResult.getTimestamp(), pendingDeploy.getDeployMarker().getUser(), Optional.empty());
      }
    }

    if (pendingDeploy.getUpdatedRequest().isPresent() && deployResult.getDeployState() == DeployState.SUCCEEDED) {
      requestManager.update(pendingDeploy.getUpdatedRequest().get(), System.currentTimeMillis(), pendingDeploy.getDeployMarker().getUser(), Optional.empty());
      requestManager.deleteExpiringObject(SingularityExpiringScale.class, request.getId());
    }

    removePendingDeploy(pendingDeploy);
  }

  private PendingType canceledOr(DeployState deployState, PendingType pendingType) {
    if (deployState == DeployState.CANCELED) {
      return PendingType.DEPLOY_CANCELLED;
    } else {
      return pendingType;
    }
  }

  private void removePendingDeploy(SingularityPendingDeploy pendingDeploy) {
    deployManager.deletePendingDeploy(pendingDeploy.getDeployMarker().getRequestId());
  }

  private void failPendingDeployDueToState(SingularityPendingDeploy pendingDeploy, Optional<SingularityRequestWithState> maybeRequestWithState, Optional<SingularityDeploy> deploy) {
    SingularityDeployResult deployResult = new SingularityDeployResult(DeployState.FAILED, Optional.of(String.format("Request in state %s is not deployable", SingularityRequestWithState.getRequestState(maybeRequestWithState))), Optional.empty());
    if (!maybeRequestWithState.isPresent()) {
      deployManager.saveDeployResult(pendingDeploy.getDeployMarker(), deploy, deployResult);
      removePendingDeploy(pendingDeploy);
      return;
    }

    saveNewDeployState(pendingDeploy.getDeployMarker(), Optional.empty());
    finishDeploy(maybeRequestWithState.get(), deploy, pendingDeploy, Collections.<SingularityTaskId>emptyList(), deployResult);
  }

  private long getAllowedMillis(SingularityDeploy deploy) {
    long seconds = deploy.getDeployHealthTimeoutSeconds().orElse(configuration.getDeployHealthyBySeconds());

    if (deploy.getHealthcheck().isPresent() && !deploy.getSkipHealthchecksOnDeploy().orElse(false)) {
      seconds += deployHealthHelper.getMaxHealthcheckTimeoutSeconds(deploy.getHealthcheck().get());
    } else {
      seconds += deploy.getConsiderHealthyAfterRunningForSeconds().orElse(configuration.getConsiderTaskHealthyAfterRunningForSeconds());
    }

    return TimeUnit.SECONDS.toMillis(seconds);
  }

  private boolean isDeployOverdue(SingularityPendingDeploy pendingDeploy, Optional<SingularityDeploy> deploy) {
    if (!deploy.isPresent()) {
      LOG.warn("Can't determine if deploy {} is overdue because it was missing", pendingDeploy);
      return false;
    }

    if (pendingDeploy.getDeployProgress().isPresent() && pendingDeploy.getDeployProgress().get().isStepComplete()) {
      return false;
    }

    final long startTime = getStartTime(pendingDeploy);

    final long deployDuration = System.currentTimeMillis() - startTime;

    final long allowedTime = getAllowedMillis(deploy.get());

    if (deployDuration > allowedTime) {
      LOG.warn("Deploy {} is overdue (duration: {}), allowed: {}", pendingDeploy, DurationFormatUtils.formatDurationHMS(deployDuration), DurationFormatUtils.formatDurationHMS(allowedTime));

      return true;
    } else {
      LOG.trace("Deploy {} is not yet overdue (duration: {}), allowed: {}", pendingDeploy, DurationFormatUtils.formatDurationHMS(deployDuration), DurationFormatUtils.formatDurationHMS(allowedTime));

      return false;
    }
  }

  private long getStartTime(SingularityPendingDeploy pendingDeploy) {
    if (pendingDeploy.getDeployProgress().isPresent()) {
      return pendingDeploy.getDeployProgress().get().getTimestamp();
    } else {
      return pendingDeploy.getDeployMarker().getTimestamp();
    }
  }

  private List<SingularityTask> getTasks(Collection<SingularityTaskId> taskIds, Map<SingularityTaskId, SingularityTask> taskIdToTask) {
    final List<SingularityTask> tasks = Lists.newArrayListWithCapacity(taskIds.size());

    for (SingularityTaskId taskId : taskIds) {
      // TODO what if one is missing?
      tasks.add(taskIdToTask.get(taskId));
    }

    return tasks;
  }

  private void updatePendingDeploy(SingularityPendingDeploy pendingDeploy, Optional<SingularityLoadBalancerUpdate> lbUpdate, DeployState deployState,
    Optional<SingularityDeployProgress> deployProgress) {
    SingularityPendingDeploy copy = new SingularityPendingDeploy(pendingDeploy.getDeployMarker(), lbUpdate, deployState, deployProgress, pendingDeploy.getUpdatedRequest());

    deployManager.savePendingDeploy(copy);
  }

  private void updatePendingDeploy(SingularityPendingDeploy pendingDeploy, Optional<SingularityLoadBalancerUpdate> lbUpdate, DeployState deployState) {
    updatePendingDeploy(pendingDeploy, lbUpdate, deployState, pendingDeploy.getDeployProgress());
  }

  private DeployState interpretLoadBalancerState(SingularityLoadBalancerUpdate lbUpdate, DeployState unknownState) {
    switch (lbUpdate.getLoadBalancerState()) {
      case CANCELED:
        return DeployState.CANCELED;
      case SUCCESS:
        return DeployState.SUCCEEDED;
      case FAILED:
      case INVALID_REQUEST_NOOP:
        return DeployState.FAILED;
      case CANCELING:
        return DeployState.CANCELING;
      case UNKNOWN:
        return unknownState;
      case WAITING:
    }

    return DeployState.WAITING;
  }

  private SingularityLoadBalancerUpdate sendCancelToLoadBalancer(SingularityPendingDeploy pendingDeploy) {
    return lbClient.cancel(getLoadBalancerRequestId(pendingDeploy));
  }

  private SingularityDeployResult cancelLoadBalancer(SingularityPendingDeploy pendingDeploy, List<SingularityDeployFailure> deployFailures) {
    final SingularityLoadBalancerUpdate lbUpdate = sendCancelToLoadBalancer(pendingDeploy);

    final DeployState deployState = interpretLoadBalancerState(lbUpdate, DeployState.CANCELING);

    updatePendingDeploy(pendingDeploy, Optional.of(lbUpdate), deployState);

    return new SingularityDeployResult(deployState, lbUpdate, deployFailures);
  }

  private boolean shouldCancelLoadBalancer(final SingularityPendingDeploy pendingDeploy) {
    return pendingDeploy.getLastLoadBalancerUpdate().isPresent() && !pendingDeploy.getCurrentDeployState().isDeployFinished();
  }

  private boolean shouldCheckLbState(final SingularityPendingDeploy pendingDeploy) {
    return pendingDeploy.getLastLoadBalancerUpdate().isPresent()
      && getLoadBalancerRequestId(pendingDeploy).getId().equals(pendingDeploy.getLastLoadBalancerUpdate().get().getLoadBalancerRequestId().getId())
      && (pendingDeploy.getLastLoadBalancerUpdate().get().getLoadBalancerState() != BaragonRequestState.UNKNOWN);
  }

  private LoadBalancerRequestId getLoadBalancerRequestId(SingularityPendingDeploy pendingDeploy) {
    return new LoadBalancerRequestId(
      String.format("%s-%s-%s", pendingDeploy.getDeployMarker().getRequestId(), pendingDeploy.getDeployMarker().getDeployId(), pendingDeploy.getDeployProgress().get().getTargetActiveInstances()),
      LoadBalancerRequestType.DEPLOY, Optional.empty());
  }

  private SingularityDeployResult getDeployResult(final SingularityRequest request, final Optional<SingularityDeployMarker> cancelRequest, final SingularityPendingDeploy pendingDeploy,
    final Optional<SingularityUpdatePendingDeployRequest> updatePendingDeployRequest, final Optional<SingularityDeploy> deploy, final Collection<SingularityTaskId> deployActiveTasks, final Collection<SingularityTaskId> otherActiveTasks,
    final Collection<SingularityTaskId> inactiveDeployMatchingTasks) {
    if (!request.isDeployable()) {
      LOG.info("Succeeding a deploy {} because the request {} was not deployable", pendingDeploy, request);

      return new SingularityDeployResult(DeployState.SUCCEEDED, "Request not deployable");
    }

    if (!pendingDeploy.getDeployProgress().isPresent()) {
      return new SingularityDeployResult(DeployState.FAILED, "No deploy progress data present in Zookeeper. Please reattempt your deploy");
    }

    Set<SingularityTaskId> newInactiveDeployTasks = getNewInactiveDeployTasks(pendingDeploy, inactiveDeployMatchingTasks);

    if (!newInactiveDeployTasks.isEmpty()) {
      if (canRetryTasks(deploy, inactiveDeployMatchingTasks)) {
        SingularityDeployProgress newProgress = pendingDeploy.getDeployProgress().get().withFailedTasks(new HashSet<>(inactiveDeployMatchingTasks));
        updatePendingDeploy(pendingDeploy, pendingDeploy.getLastLoadBalancerUpdate(), DeployState.WAITING, Optional.of(newProgress));
        requestManager.addToPendingQueue(
          new SingularityPendingRequest(request.getId(), pendingDeploy.getDeployMarker().getDeployId(), System.currentTimeMillis(), pendingDeploy.getDeployMarker().getUser(),
            PendingType.NEXT_DEPLOY_STEP, deploy.isPresent() ? deploy.get().getSkipHealthchecksOnDeploy() : Optional.empty(),
            pendingDeploy.getDeployMarker().getMessage()));
        return new SingularityDeployResult(DeployState.WAITING);
      }

      if (request.isLoadBalanced() && shouldCancelLoadBalancer(pendingDeploy)) {
        LOG.info("Attempting to cancel pending load balancer request, failing deploy {} regardless", pendingDeploy);
        sendCancelToLoadBalancer(pendingDeploy);
      }

      int maxRetries = deploy.get().getMaxTaskRetries().orElse(configuration.getDefaultDeployMaxTaskRetries());
      return getDeployResultWithFailures(request, deploy, pendingDeploy, DeployState.FAILED, String.format("%s task(s) for this deploy failed", inactiveDeployMatchingTasks.size() - maxRetries), inactiveDeployMatchingTasks);
    }

    return checkDeployProgress(request, cancelRequest, pendingDeploy, updatePendingDeployRequest, deploy, deployActiveTasks, otherActiveTasks);
  }

  private boolean canRetryTasks(Optional<SingularityDeploy> deploy, Collection<SingularityTaskId> inactiveDeployMatchingTasks) {
    int maxRetries = deploy.get().getMaxTaskRetries().orElse(configuration.getDefaultDeployMaxTaskRetries());
    return deploy.isPresent() && maxRetries > 0 && inactiveDeployMatchingTasks.size() <= maxRetries;
  }

  private Set<SingularityTaskId> getNewInactiveDeployTasks(SingularityPendingDeploy pendingDeploy, Collection<SingularityTaskId> inactiveDeployMatchingTasks) {
    Set<SingularityTaskId> newInactiveDeployTasks = new HashSet<>();
    newInactiveDeployTasks.addAll(inactiveDeployMatchingTasks);

    if (pendingDeploy.getDeployProgress().isPresent()) {
      newInactiveDeployTasks.removeAll(pendingDeploy.getDeployProgress().get().getFailedDeployTasks());
    }

    return newInactiveDeployTasks;
  }

  private SingularityDeployResult checkDeployProgress(final SingularityRequest request, final Optional<SingularityDeployMarker> cancelRequest, final SingularityPendingDeploy pendingDeploy,
    final Optional<SingularityUpdatePendingDeployRequest> updatePendingDeployRequest, final Optional<SingularityDeploy> deploy, final Collection<SingularityTaskId> deployActiveTasks,
    final Collection<SingularityTaskId> otherActiveTasks) {
    SingularityDeployProgress deployProgress = pendingDeploy.getDeployProgress().get();

    if (cancelRequest.isPresent()) {
      LOG.info("Canceling a deploy {} due to cancel request {}", pendingDeploy, cancelRequest.get());
      String userMessage = cancelRequest.get().getUser().isPresent() ? String.format(" by %s", cancelRequest.get().getUser().get()) : "";
      return new SingularityDeployResult(DeployState.CANCELED, Optional.of(String.format("Canceled due to request%s at %s", userMessage, cancelRequest.get().getTimestamp())),
        pendingDeploy.getLastLoadBalancerUpdate(), Collections.<SingularityDeployFailure>emptyList(), System.currentTimeMillis());
    }

    if (deployProgress.isStepComplete()) {
      return checkCanMoveToNextDeployStep(request, deploy, pendingDeploy, updatePendingDeployRequest);
    }

    final boolean isDeployOverdue = isDeployOverdue(pendingDeploy, deploy);
    if (deployActiveTasks.size() < deployProgress.getTargetActiveInstances()) {
      maybeUpdatePendingRequest(pendingDeploy, deploy, request, updatePendingDeployRequest);
      return checkOverdue(request, deploy, pendingDeploy, deployActiveTasks, isDeployOverdue);
    }

    if (shouldCheckLbState(pendingDeploy)) {
      final SingularityLoadBalancerUpdate lbUpdate = lbClient.getState(getLoadBalancerRequestId(pendingDeploy));
      return processLbState(request, deploy, pendingDeploy, updatePendingDeployRequest, deployActiveTasks, otherActiveTasks, tasksToShutDown(deployProgress, otherActiveTasks, request), lbUpdate);
    }

    if (isDeployOverdue && request.isLoadBalanced() && shouldCancelLoadBalancer(pendingDeploy)) {
      return cancelLoadBalancer(pendingDeploy, getDeployFailures(request, deploy, pendingDeploy, DeployState.OVERDUE, deployActiveTasks));
    }

    if (isWaitingForCurrentLbRequest(pendingDeploy)) {
      return new SingularityDeployResult(DeployState.WAITING, Optional.of("Waiting on load balancer API"), pendingDeploy.getLastLoadBalancerUpdate());
    }

    final DeployHealth deployHealth = deployHealthHelper.getDeployHealth(request, deploy, deployActiveTasks, true);
    switch (deployHealth) {
      case WAITING:
        maybeUpdatePendingRequest(pendingDeploy, deploy, request, updatePendingDeployRequest);
        return checkOverdue(request, deploy, pendingDeploy, deployActiveTasks, isDeployOverdue);
      case HEALTHY:
        if (!request.isLoadBalanced()) {
          return markStepFinished(pendingDeploy, deploy, deployActiveTasks, otherActiveTasks, request, updatePendingDeployRequest);
        }

        if (updatePendingDeployRequest.isPresent() && updatePendingDeployRequest.get().getTargetActiveInstances() != deployProgress.getTargetActiveInstances()) {
          maybeUpdatePendingRequest(pendingDeploy, deploy, request, updatePendingDeployRequest);
          return new SingularityDeployResult(DeployState.WAITING);
        }

        if (configuration.getLoadBalancerUri() == null) {
          LOG.warn("Deploy {} required a load balancer URI but it wasn't set", pendingDeploy);
          return new SingularityDeployResult(DeployState.FAILED, Optional.of("No valid load balancer URI was present"), Optional.empty(), Collections.<SingularityDeployFailure>emptyList(), System.currentTimeMillis());
        }

        for (SingularityTaskId activeTaskId : deployActiveTasks) {
          taskManager.markHealthchecksFinished(activeTaskId);
          taskManager.clearStartupHealthchecks(activeTaskId);
        }

        return enqueueAndProcessLbRequest(request, deploy, pendingDeploy, updatePendingDeployRequest, deployActiveTasks, otherActiveTasks);
      case UNHEALTHY:
      default:
        for (SingularityTaskId activeTaskId : deployActiveTasks) {
          taskManager.markHealthchecksFinished(activeTaskId);
          taskManager.clearStartupHealthchecks(activeTaskId);
        }
        return getDeployResultWithFailures(request, deploy, pendingDeploy, DeployState.FAILED, "Not all tasks for deploy were healthy", deployActiveTasks);
    }
  }

  private SingularityDeployResult checkCanMoveToNextDeployStep(SingularityRequest request, Optional<SingularityDeploy> deploy, SingularityPendingDeploy pendingDeploy,
    Optional<SingularityUpdatePendingDeployRequest> updatePendingDeployRequest) {
    SingularityDeployProgress deployProgress = pendingDeploy.getDeployProgress().get();
    if (canMoveToNextStep(deployProgress) || updatePendingDeployRequest.isPresent()) {
      SingularityDeployProgress newProgress = deployProgress.withNewTargetInstances(getNewTargetInstances(deployProgress, request, updatePendingDeployRequest));
      updatePendingDeploy(pendingDeploy, pendingDeploy.getLastLoadBalancerUpdate(), DeployState.WAITING, Optional.of(newProgress));
      requestManager.addToPendingQueue(
        new SingularityPendingRequest(request.getId(), pendingDeploy.getDeployMarker().getDeployId(), System.currentTimeMillis(), pendingDeploy.getDeployMarker().getUser(),
          PendingType.NEXT_DEPLOY_STEP, deploy.isPresent() ? deploy.get().getSkipHealthchecksOnDeploy() : Optional.empty(), pendingDeploy.getDeployMarker().getMessage()));
    }
    return new SingularityDeployResult(DeployState.WAITING);
  }

  private SingularityDeployResult enqueueAndProcessLbRequest(SingularityRequest request, Optional<SingularityDeploy> deploy, SingularityPendingDeploy pendingDeploy,
    Optional<SingularityUpdatePendingDeployRequest> updatePendingDeployRequest, Collection<SingularityTaskId> deployActiveTasks, Collection<SingularityTaskId> otherActiveTasks) {
    Collection<SingularityTaskId> toShutDown = tasksToShutDown(pendingDeploy.getDeployProgress().get(), otherActiveTasks, request);
    final Map<SingularityTaskId, SingularityTask> tasks = taskManager.getTasks(Iterables.concat(deployActiveTasks, toShutDown));
    final LoadBalancerRequestId lbRequestId = getLoadBalancerRequestId(pendingDeploy);

    List<SingularityTaskId> toRemoveFromLb = new ArrayList<>();
    for (SingularityTaskId taskId : toShutDown) {
      Optional<SingularityLoadBalancerUpdate> maybeAddUpdate = taskManager.getLoadBalancerState(taskId, LoadBalancerRequestType.ADD);
      if (maybeAddUpdate.isPresent() && maybeAddUpdate.get().getLoadBalancerState() == BaragonRequestState.SUCCESS) {
        toRemoveFromLb.add(taskId);
      }
    }

    updateLoadBalancerStateForTasks(deployActiveTasks, LoadBalancerRequestType.ADD, SingularityLoadBalancerUpdate.preEnqueue(lbRequestId));
    updateLoadBalancerStateForTasks(toRemoveFromLb, LoadBalancerRequestType.REMOVE, SingularityLoadBalancerUpdate.preEnqueue(lbRequestId));
    SingularityLoadBalancerUpdate enqueueResult = lbClient.enqueue(lbRequestId, request, deploy.get(), getTasks(deployActiveTasks, tasks), getTasks(toShutDown, tasks));
    return processLbState(request, deploy, pendingDeploy, updatePendingDeployRequest, deployActiveTasks, otherActiveTasks, toShutDown, enqueueResult);
  }

  private SingularityDeployResult processLbState(SingularityRequest request, Optional<SingularityDeploy> deploy, SingularityPendingDeploy pendingDeploy,
    Optional<SingularityUpdatePendingDeployRequest> updatePendingDeployRequest, Collection<SingularityTaskId> deployActiveTasks, Collection<SingularityTaskId> otherActiveTasks,
    Collection<SingularityTaskId> tasksToShutDown, SingularityLoadBalancerUpdate lbUpdate) {

    List<SingularityTaskId> toRemoveFromLb = new ArrayList<>();
    for (SingularityTaskId taskId : tasksToShutDown) {
      Optional<SingularityLoadBalancerUpdate> maybeRemoveUpdate = taskManager.getLoadBalancerState(taskId, LoadBalancerRequestType.REMOVE);
      if (maybeRemoveUpdate.isPresent() && maybeRemoveUpdate.get().getLoadBalancerRequestId().getId().equals(lbUpdate.getLoadBalancerRequestId().getId())) {
        toRemoveFromLb.add(taskId);
      }
    }

    updateLoadBalancerStateForTasks(deployActiveTasks, LoadBalancerRequestType.ADD, lbUpdate);
    updateLoadBalancerStateForTasks(toRemoveFromLb, LoadBalancerRequestType.REMOVE, lbUpdate);

    DeployState deployState = interpretLoadBalancerState(lbUpdate, pendingDeploy.getCurrentDeployState());
    if (deployState == DeployState.SUCCEEDED) {
      updatePendingDeploy(pendingDeploy, Optional.of(lbUpdate), DeployState.WAITING); // A step has completed, markStepFinished will determine SUCCEEDED/WAITING
      return markStepFinished(pendingDeploy, deploy, deployActiveTasks, otherActiveTasks, request, updatePendingDeployRequest);
    } else if (deployState == DeployState.WAITING) {
      updatePendingDeploy(pendingDeploy, Optional.of(lbUpdate), deployState);
      maybeUpdatePendingRequest(pendingDeploy, deploy, request, updatePendingDeployRequest, Optional.of(lbUpdate));
      return new SingularityDeployResult(DeployState.WAITING);
    } else {
      updatePendingDeploy(pendingDeploy, Optional.of(lbUpdate), deployState);
      maybeUpdatePendingRequest(pendingDeploy, deploy, request, updatePendingDeployRequest, Optional.of(lbUpdate));
      return new SingularityDeployResult(deployState, lbUpdate, SingularityDeployFailure.lbUpdateFailed());
    }
  }

  private void maybeUpdatePendingRequest(SingularityPendingDeploy pendingDeploy, Optional<SingularityDeploy> deploy, SingularityRequest request,
    Optional<SingularityUpdatePendingDeployRequest> updatePendingDeployRequest) {
    maybeUpdatePendingRequest(pendingDeploy, deploy, request, updatePendingDeployRequest, Optional.empty());
  }

  private void maybeUpdatePendingRequest(SingularityPendingDeploy pendingDeploy, Optional<SingularityDeploy> deploy, SingularityRequest request,
    Optional<SingularityUpdatePendingDeployRequest> updatePendingDeployRequest, Optional<SingularityLoadBalancerUpdate> lbUpdate) {
    if (updatePendingDeployRequest.isPresent() && pendingDeploy.getDeployProgress().isPresent()) {
      SingularityDeployProgress newProgress =
        pendingDeploy.getDeployProgress().get().withNewTargetInstances(Math.min(updatePendingDeployRequest.get().getTargetActiveInstances(), request.getInstancesSafe()));
      updatePendingDeploy(pendingDeploy, lbUpdate.isPresent() ? lbUpdate : pendingDeploy.getLastLoadBalancerUpdate(), DeployState.WAITING, Optional.of(newProgress));
      requestManager
        .addToPendingQueue(new SingularityPendingRequest(request.getId(), pendingDeploy.getDeployMarker().getDeployId(), System.currentTimeMillis(), pendingDeploy.getDeployMarker().getUser(),
          PendingType.NEXT_DEPLOY_STEP, deploy.isPresent() ? deploy.get().getSkipHealthchecksOnDeploy() : Optional.empty(),
          pendingDeploy.getDeployMarker().getMessage()));
    }
  }

  private boolean isWaitingForCurrentLbRequest(SingularityPendingDeploy pendingDeploy) {
    return pendingDeploy.getLastLoadBalancerUpdate().isPresent()
      && getLoadBalancerRequestId(pendingDeploy).getId().equals(pendingDeploy.getLastLoadBalancerUpdate().get().getLoadBalancerRequestId().getId())
      && pendingDeploy.getLastLoadBalancerUpdate().get().getLoadBalancerState() == BaragonRequestState.WAITING;
  }

  private boolean isLastStepFinished(SingularityDeployProgress deployProgress, SingularityRequest request) {
    return deployProgress.isStepComplete() && deployProgress.getTargetActiveInstances() >= request.getInstancesSafe();
  }

  private SingularityDeployResult markStepFinished(SingularityPendingDeploy pendingDeploy, Optional<SingularityDeploy> deploy, Collection<SingularityTaskId> deployActiveTasks,
                                                   Collection<SingularityTaskId> otherActiveTasks, SingularityRequest request,
                                                   Optional<SingularityUpdatePendingDeployRequest> updatePendingDeployRequest) {
    SingularityDeployProgress deployProgress = pendingDeploy.getDeployProgress().get();

    if (updatePendingDeployRequest.isPresent() && getNewTargetInstances(deployProgress, request, updatePendingDeployRequest) != deployProgress.getTargetActiveInstances()) {
      maybeUpdatePendingRequest(pendingDeploy, deploy, request, updatePendingDeployRequest);
      return new SingularityDeployResult(DeployState.WAITING);
    }

    SingularityDeployProgress newProgress = deployProgress.withNewActiveInstances(deployActiveTasks.size()).withCompletedStep();
    DeployState deployState = isLastStepFinished(newProgress, request) ? DeployState.SUCCEEDED : DeployState.WAITING;

    String message = deployState == DeployState.SUCCEEDED ? "New deploy succeeded" : "New deploy is progressing, this task is being replaced";

    updatePendingDeploy(pendingDeploy, pendingDeploy.getLastLoadBalancerUpdate(), deployState, Optional.of(newProgress));
    for (SingularityTaskId taskId : tasksToShutDown(deployProgress, otherActiveTasks, request)) {
      taskManager.createTaskCleanup(
        new SingularityTaskCleanup(Optional.empty(), TaskCleanupType.DEPLOY_STEP_FINISHED, System.currentTimeMillis(), taskId, Optional.of(message),
          Optional.empty(), Optional.empty()));
    }
    return new SingularityDeployResult(deployState);
  }

  private List<SingularityTaskId> tasksToShutDown(SingularityDeployProgress deployProgress, Collection<SingularityTaskId> otherActiveTasks, SingularityRequest request) {
    int numTasksToShutDown = Math.max(otherActiveTasks.size() - (request.getInstancesSafe() - deployProgress.getTargetActiveInstances()), 0);
    List<SingularityTaskId> sortedOtherTasks = new ArrayList<>(otherActiveTasks);
    Collections.sort(sortedOtherTasks, SingularityTaskId.INSTANCE_NO_COMPARATOR);
    return sortedOtherTasks.isEmpty() ? sortedOtherTasks : sortedOtherTasks.subList(0, Math.min(numTasksToShutDown, sortedOtherTasks.size()));
  }

  private boolean canMoveToNextStep(SingularityDeployProgress deployProgress) {
    return deployProgress.isAutoAdvanceDeploySteps() && deployProgress.getTimestamp() + deployProgress.getDeployStepWaitTimeMs() < System.currentTimeMillis();
  }

  private int getNewTargetInstances(SingularityDeployProgress deployProgress, SingularityRequest request, Optional<SingularityUpdatePendingDeployRequest> updateRequest) {
    if (updateRequest.isPresent()) {
      return Math.min(updateRequest.get().getTargetActiveInstances(), request.getInstancesSafe());
    } else {
      return Math.min(deployProgress.getTargetActiveInstances() + deployProgress.getDeployInstanceCountPerStep(), request.getInstancesSafe());
    }
  }

  private SingularityDeployResult checkOverdue(SingularityRequest request, Optional<SingularityDeploy> deploy, SingularityPendingDeploy pendingDeploy, Collection<SingularityTaskId> deployActiveTasks, boolean isOverdue) {
    String message = null;

    if (deploy.isPresent()) {
      message =
        String.format("Deploy was able to launch %s tasks, but not all of them became healthy within %s", deployActiveTasks.size(), JavaUtils.durationFromMillis(getAllowedMillis(deploy.get())));
    }

    if (deploy.isPresent() && isOverdue) {
      return getDeployResultWithFailures(request, deploy, pendingDeploy, DeployState.OVERDUE, message, deployActiveTasks);
    } else {
      return new SingularityDeployResult(DeployState.WAITING);
    }
  }

  private SingularityDeployResult getDeployResultWithFailures(SingularityRequest request, Optional<SingularityDeploy> deploy, SingularityPendingDeploy pendingDeploy, DeployState state, String message, Collection<SingularityTaskId> matchingTasks) {
    List<SingularityDeployFailure> deployFailures = getDeployFailures(request, deploy, pendingDeploy, state, matchingTasks);
    if (deployFailures.size() == 1 && !deployFailures.get(0).getTaskId().isPresent()) { // Single non-task-specific failure should become the deploy result message (e.g. not enough resources to launch all tasks)
      return new SingularityDeployResult(state, deployFailures.get(0).getMessage() , pendingDeploy.getLastLoadBalancerUpdate(), Collections.<SingularityDeployFailure>emptyList(), System.currentTimeMillis());
    } else {
      return new SingularityDeployResult(state, Optional.of(message), pendingDeploy.getLastLoadBalancerUpdate(), deployFailures, System.currentTimeMillis());
    }
  }

  private List<SingularityDeployFailure> getDeployFailures(SingularityRequest request, Optional<SingularityDeploy> deploy, SingularityPendingDeploy pendingDeploy, DeployState state, Collection<SingularityTaskId> matchingTasks) {
    List<SingularityDeployFailure> failures = new ArrayList<>();
    failures.addAll(deployHealthHelper.getTaskFailures(deploy, matchingTasks));

    if (state == DeployState.OVERDUE) {
      int targetInstances = pendingDeploy.getDeployProgress().isPresent() ? pendingDeploy.getDeployProgress().get().getTargetActiveInstances() :request.getInstancesSafe();
      if (failures.isEmpty() && matchingTasks.size() < targetInstances) {
        failures.add(new SingularityDeployFailure(SingularityDeployFailureReason.TASK_COULD_NOT_BE_SCHEDULED, Optional.empty(), Optional.of(String.format("Only %s of %s tasks could be launched for deploy, there may not be enough resources to launch the remaining tasks", matchingTasks.size(), targetInstances))));
      }
    }

    return failures;
  }
}
