package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.LoadBalancerRequestType;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityDeployResult;
import com.hubspot.singularity.SingularityDeployProgress;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityLoadBalancerUpdate.LoadBalancerMethod;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityUpdatePendingDeployRequest;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.hooks.LoadBalancerClient;
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

  @Inject
  public SingularityDeployChecker(DeployManager deployManager, SingularityDeployHealthHelper deployHealthHelper, LoadBalancerClient lbClient, RequestManager requestManager, TaskManager taskManager, SingularityConfiguration configuration) {
    this.configuration = configuration;
    this.lbClient = lbClient;
    this.deployHealthHelper = deployHealthHelper;
    this.requestManager = requestManager;
    this.deployManager = deployManager;
    this.taskManager = taskManager;
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

    for (SingularityPendingDeploy pendingDeploy : pendingDeploys) {
      LOG.debug("Checking a deploy {}", pendingDeploy);

      checkDeploy(pendingDeploy, cancelDeploys, pendingDeployToKey, deployKeyToDeploy, updateRequests);
    }

    for (SingularityDeployMarker cancelDeploy : cancelDeploys) {
      SingularityDeleteResult deleteResult = deployManager.deleteCancelDeployRequest(cancelDeploy);

      LOG.debug("Removing cancel deploy request {} - {}", cancelDeploy, deleteResult);
    }

    for(SingularityUpdatePendingDeployRequest updateRequest : updateRequests) {
      SingularityDeleteResult deleteResult = deployManager.deleteUpdatePendingDeployRequest(updateRequest);
      LOG.debug("Removing request to update pending deploy {} - {}", updateRequest, deleteResult);
    }

    return pendingDeploys.size();
  }

  private void checkDeploy(final SingularityPendingDeploy pendingDeploy, final List<SingularityDeployMarker> cancelDeploys, final Map<SingularityPendingDeploy, SingularityDeployKey> pendingDeployToKey, final Map<SingularityDeployKey, SingularityDeploy> deployKeyToDeploy, List<SingularityUpdatePendingDeployRequest> updateRequests) {
    final SingularityDeployKey deployKey = pendingDeployToKey.get(pendingDeploy);
    final Optional<SingularityDeploy> deploy = Optional.fromNullable(deployKeyToDeploy.get(deployKey));

    Optional<SingularityRequestWithState> maybeRequestWithState = requestManager.getRequest(pendingDeploy.getDeployMarker().getRequestId());

    if (!SingularityRequestWithState.isActive(maybeRequestWithState)) {
      LOG.warn("Deploy {} request was {}, removing deploy", SingularityRequestWithState.getRequestState(maybeRequestWithState), pendingDeploy);

      if (shouldCancelLoadBalancer(pendingDeploy)) {
        cancelLoadBalancer(pendingDeploy);
      }

      removePendingDeploy(pendingDeploy);
      return;
    }

    final SingularityDeployMarker pendingDeployMarker = pendingDeploy.getDeployMarker();

    final Optional<SingularityDeployMarker> cancelRequest = findCancel(cancelDeploys, pendingDeployMarker);
    final Optional<SingularityUpdatePendingDeployRequest> updatePendingDeployRequest = findUpdateRequest(updateRequests, pendingDeploy);

    final SingularityRequestWithState requestWithState = maybeRequestWithState.get();
    final SingularityRequest request = requestWithState.getRequest();

    final List<SingularityTaskId> requestTasks = taskManager.getTaskIdsForRequest(request.getId());
    final List<SingularityTaskId> activeTasks = taskManager.filterActiveTaskIds(requestTasks);

    final List<SingularityTaskId> inactiveDeployMatchingTasks = SingularityTaskId.matchingAndNotIn(requestTasks, pendingDeployMarker.getRequestId(), pendingDeployMarker.getDeployId(), activeTasks);
    final List<SingularityTaskId> deployMatchingTasks = Lists.newArrayList(Iterables.filter(activeTasks, SingularityTaskId.matchingDeploy(pendingDeployMarker.getDeployId())));
    final List<SingularityTaskId> allOtherMatchingTasks = Lists.newArrayList(Iterables.filter(activeTasks, Predicates.not(SingularityTaskId.matchingDeploy(pendingDeployMarker.getDeployId()))));

    SingularityDeployResult deployResult = getDeployResult(request, cancelRequest, pendingDeploy, updatePendingDeployRequest, deployKey, deploy, deployMatchingTasks, allOtherMatchingTasks, inactiveDeployMatchingTasks);

    LOG.info("Deploy {} had result {} after {}", pendingDeployMarker, deployResult, JavaUtils.durationFromMillis(System.currentTimeMillis() - pendingDeployMarker.getTimestamp()));

    if (deployResult.getDeployState() == DeployState.SUCCEEDED) {
      if (saveNewDeployState(pendingDeployMarker, Optional.of(pendingDeployMarker))) {
        deleteObsoletePendingTasks(pendingDeploy);
        finishDeploy(requestWithState, deploy, pendingDeploy, allOtherMatchingTasks, deployResult);
        return;
      } else {
        LOG.warn("Failing deploy {} because it failed to save deploy state", pendingDeployMarker);
        deployResult = new SingularityDeployResult(DeployState.FAILED_INTERNAL_STATE, Optional.of(String.format("Deploy had state %s but failed to persist it correctly", deployResult.getDeployState())), deployResult.getLbUpdate(), deployResult.getTimestamp());
      }
    } else if (!deployResult.getDeployState().isDeployFinished()) {
      return;
    }

    // success case is handled, handle failure cases:
    saveNewDeployState(pendingDeployMarker, Optional.<SingularityDeployMarker> absent());
    finishDeploy(requestWithState, deploy, pendingDeploy, deployMatchingTasks, deployResult);
  }

  private void deleteObsoletePendingTasks(SingularityPendingDeploy pendingDeploy) {
    for (SingularityPendingTaskId pendingTaskId : Iterables.filter(taskManager.getPendingTaskIds(), Predicates.and(SingularityPendingTaskId.matchingRequestId(pendingDeploy.getDeployMarker().getRequestId()), Predicates.not(SingularityPendingTaskId.matchingDeployId(pendingDeploy.getDeployMarker().getDeployId()))))) {
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

    return Optional.absent();
  }

  private Optional<SingularityUpdatePendingDeployRequest> findUpdateRequest(List<SingularityUpdatePendingDeployRequest> updateRequests, SingularityPendingDeploy pendingDeploy) {
    for (SingularityUpdatePendingDeployRequest updateRequest : updateRequests) {
      if (updateRequest.getRequestId().equals(pendingDeploy.getDeployMarker().getRequestId())&& updateRequest.getDeployId().equals(pendingDeploy.getDeployMarker().getDeployId())) {
        return Optional.of(updateRequest);
      }
    }
    return Optional.absent();
  }

  private void updateLoadBalancerStateForTasks(Collection<SingularityTaskId> taskIds, LoadBalancerRequestType type, SingularityLoadBalancerUpdate update) {
    for (SingularityTaskId taskId : taskIds) {
      taskManager.saveLoadBalancerState(taskId, type, update);
    }
  }

  private void cleanupTasks(SingularityDeployMarker deployMarker, SingularityDeployResult deployResult, Iterable<SingularityTaskId> tasksToKill) {
    for (SingularityTaskId matchingTask : tasksToKill) {
      taskManager.createTaskCleanup(new SingularityTaskCleanup(deployMarker.getUser(), deployResult.getDeployState().getCleanupType(), deployResult.getTimestamp(), matchingTask,
          Optional.of(String.format("Deploy %s - %s", deployMarker.getDeployId(), deployResult.getDeployState().name())), Optional.<String> absent()));
    }
  }

  private boolean saveNewDeployState(SingularityDeployMarker pendingDeployMarker, Optional<SingularityDeployMarker> newActiveDeploy) {
    Optional<SingularityRequestDeployState> deployState = deployManager.getRequestDeployState(pendingDeployMarker.getRequestId());

    if (!deployState.isPresent()) {
      LOG.error("Expected deploy state for deploy marker: {} but didn't find it", pendingDeployMarker);
      return false;
    }

    deployManager.saveNewRequestDeployState(new SingularityRequestDeployState(deployState.get().getRequestId(), newActiveDeploy.or(deployState.get().getActiveDeploy()),
        Optional.<SingularityDeployMarker> absent()));

    return true;
  }

  private void finishDeploy(SingularityRequestWithState requestWithState, Optional<SingularityDeploy> deploy, SingularityPendingDeploy pendingDeploy, Iterable<SingularityTaskId> tasksToKill, SingularityDeployResult deployResult) {
    SingularityRequest request = requestWithState.getRequest();

    if (!request.isOneOff()) {
      cleanupTasks(pendingDeploy.getDeployMarker(), deployResult, tasksToKill);
    }

    if (!request.isDeployable() && !request.isOneOff()) {
      // TODO should this override? What if someone has mucked with the pending queue for this deploy ?
      requestManager.addToPendingQueue(new SingularityPendingRequest(request.getId(), pendingDeploy.getDeployMarker().getDeployId(), deployResult.getTimestamp(),
        pendingDeploy.getDeployMarker().getUser(), PendingType.NEXT_DEPLOY_STEP, deploy.isPresent() ? deploy.get().getSkipHealthchecksOnDeploy() : Optional.<Boolean> absent(),
        pendingDeploy.getDeployMarker().getMessage()));
    }

    deployManager.saveDeployResult(pendingDeploy.getDeployMarker(), deploy, deployResult);

    if (requestWithState.getState() == RequestState.DEPLOYING_TO_UNPAUSE) {
      if (deployResult.getDeployState() == DeployState.SUCCEEDED) {
        requestManager.activate(request, RequestHistoryType.DEPLOYED_TO_UNPAUSE, deployResult.getTimestamp(), pendingDeploy.getDeployMarker().getUser(), Optional.<String> absent());
      } else {
        requestManager.pause(request, deployResult.getTimestamp(), pendingDeploy.getDeployMarker().getUser(), Optional.<String> absent());
      }
    }

    removePendingDeploy(pendingDeploy);
  }

  private void removePendingDeploy(SingularityPendingDeploy pendingDeploy) {
    deployManager.deletePendingDeploy(pendingDeploy.getDeployMarker().getRequestId());
  }

  private long getAllowedMillis(SingularityDeploy deploy) {
    long seconds = deploy.getDeployHealthTimeoutSeconds().or(configuration.getDeployHealthyBySeconds());

    if (deploy.getHealthcheckUri().isPresent() && !deploy.getSkipHealthchecksOnDeploy().or(false)) {
      seconds += deploy.getHealthcheckIntervalSeconds().or(configuration.getHealthcheckIntervalSeconds()) + deploy.getHealthcheckTimeoutSeconds().or(configuration.getHealthcheckTimeoutSeconds());
    } else {
      seconds += deploy.getConsiderHealthyAfterRunningForSeconds().or(configuration.getConsiderTaskHealthyAfterRunningForSeconds());
    }

    return TimeUnit.SECONDS.toMillis(seconds);
  }

  private boolean isDeployOverdue(SingularityPendingDeploy pendingDeploy, Optional<SingularityDeploy> deploy) {
    if (!deploy.isPresent()) {
      LOG.warn("Can't determine if deploy {} is overdue because it was missing", pendingDeploy);

      return false;
    }

    final long startTime;
    if (pendingDeploy.getDeployProgress().isPresent()) {
      if (pendingDeploy.getDeployProgress().get().isStepComplete()) {
        return false;
      } else {
        startTime = pendingDeploy.getDeployProgress().get().getTimestamp();
      }
    } else {
      startTime = pendingDeploy.getDeployMarker().getTimestamp();
    }

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

  private List<SingularityTask> getTasks(Collection<SingularityTaskId> taskIds, Map<SingularityTaskId, SingularityTask> taskIdToTask) {
    final List<SingularityTask> tasks = Lists.newArrayListWithCapacity(taskIds.size());

    for (SingularityTaskId taskId : taskIds) {
      // TODO what if one is missing?
      tasks.add(taskIdToTask.get(taskId));
    }

    return tasks;
  }

  private void updatePendingDeploy(SingularityPendingDeploy pendingDeploy, Optional<SingularityLoadBalancerUpdate> lbUpdate, DeployState deployState, Optional<SingularityDeployProgress> deployProgress) {
    SingularityPendingDeploy copy = new SingularityPendingDeploy(pendingDeploy.getDeployMarker(), lbUpdate, deployState, deployProgress);

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

  private SingularityDeployResult cancelLoadBalancer(SingularityPendingDeploy pendingDeploy) {
    final SingularityLoadBalancerUpdate lbUpdate = sendCancelToLoadBalancer(pendingDeploy);

    final DeployState deployState = interpretLoadBalancerState(lbUpdate, DeployState.CANCELING);

    updatePendingDeploy(pendingDeploy, Optional.of(lbUpdate), deployState);

    return fromLbState(deployState, lbUpdate);
  }

  private boolean shouldCancelLoadBalancer(final SingularityPendingDeploy pendingDeploy) {
    return pendingDeploy.getLastLoadBalancerUpdate().isPresent() && !pendingDeploy.getCurrentDeployState().isDeployFinished();
  }

  private SingularityDeployResult fromLbState(DeployState state, SingularityLoadBalancerUpdate lbUpdate) {
    return new SingularityDeployResult(state, lbUpdate);
  }

  private boolean shouldCheckLbState(final SingularityPendingDeploy pendingDeploy) {
    return pendingDeploy.getLastLoadBalancerUpdate().isPresent()
        && getLoadBalancerRequestId(pendingDeploy).getId().equals(pendingDeploy.getLastLoadBalancerUpdate().get().getLoadBalancerRequestId().getId())
        && (pendingDeploy.getLastLoadBalancerUpdate().get().getLoadBalancerState() != BaragonRequestState.UNKNOWN);
  }

  private LoadBalancerRequestId getLoadBalancerRequestId(SingularityPendingDeploy pendingDeploy) {
    return new LoadBalancerRequestId(String.format("%s-%s-%s", pendingDeploy.getDeployMarker().getRequestId(), pendingDeploy.getDeployMarker().getDeployId(), pendingDeploy.getDeployProgress().get().getTargetActiveInstances()), LoadBalancerRequestType.DEPLOY, Optional.<Integer> absent());
  }

  private SingularityDeployResult getDeployResult(final SingularityRequest request, final Optional<SingularityDeployMarker> cancelRequest, final SingularityPendingDeploy pendingDeploy, final Optional<SingularityUpdatePendingDeployRequest> updatePendingDeployRequest,
      final SingularityDeployKey deployKey, final Optional<SingularityDeploy> deploy, final Collection<SingularityTaskId> deployActiveTasks, final Collection<SingularityTaskId> otherActiveTasks, final Collection<SingularityTaskId> inactiveDeployMatchingTasks) {
    if (!request.isDeployable()) {
      LOG.info("Succeeding a deploy {} because the request {} was not deployable", pendingDeploy, request);

      return new SingularityDeployResult(DeployState.SUCCEEDED, "Request not deployable");
    }

    if (!inactiveDeployMatchingTasks.isEmpty()) {
      if (request.isLoadBalanced() && shouldCancelLoadBalancer(pendingDeploy)) {
        LOG.info("Attempting to cancel pending load balancer request, failing deploy {} regardless", pendingDeploy);
        sendCancelToLoadBalancer(pendingDeploy);
      }

      return new SingularityDeployResult(DeployState.FAILED, String.format("Task(s) %s for this deploy failed", inactiveDeployMatchingTasks));
    }

    if (pendingDeploy.getDeployProgress().isPresent()) {
      return checkDeployProgress(request, cancelRequest, pendingDeploy, updatePendingDeployRequest, deploy, deployActiveTasks, otherActiveTasks);
    } else {
      return new SingularityDeployResult(DeployState.FAILED, "No deploy progress present for deploy of long running request");
    }
  }

  private SingularityDeployResult checkDeployProgress(final SingularityRequest request, final Optional<SingularityDeployMarker> cancelRequest, final SingularityPendingDeploy pendingDeploy, final Optional<SingularityUpdatePendingDeployRequest> updatePendingDeployRequest,
                                                      final Optional<SingularityDeploy> deploy, final Collection<SingularityTaskId> deployActiveTasks, final Collection<SingularityTaskId> otherActiveTasks) {
    SingularityDeployProgress deployProgress = pendingDeploy.getDeployProgress().get();

    if (deployProgress.isStepComplete()) {
      if (canMoveToNextStep(deployProgress) || updatePendingDeployRequest.isPresent()) {
        SingularityDeployProgress newProgress = new SingularityDeployProgress(
            getNewTargetInstances(deployProgress, request, updatePendingDeployRequest),
            deployProgress.getDeployInstanceCountPerStep(),
            deployProgress.getDeployStepWaitTimeSeconds(),
            false,
            deployProgress.isAutoAdvanceDeploySteps(),
            System.currentTimeMillis()
        );
        updatePendingDeploy(pendingDeploy, pendingDeploy.getLastLoadBalancerUpdate(), DeployState.WAITING, Optional.of(newProgress));
        requestManager.addToPendingQueue(new SingularityPendingRequest(request.getId(), pendingDeploy.getDeployMarker().getDeployId(), System.currentTimeMillis(), pendingDeploy.getDeployMarker().getUser(),
          PendingType.NEXT_DEPLOY_STEP, deploy.isPresent() ? deploy.get().getSkipHealthchecksOnDeploy() : Optional.<Boolean> absent(),
          pendingDeploy.getDeployMarker().getMessage()));
      }
      return new SingularityDeployResult(DeployState.WAITING);
    } else {
      final boolean isCancelRequestPresent = cancelRequest.isPresent();
      final boolean isDeployOverdue = isDeployOverdue(pendingDeploy, deploy);
      if (deployActiveTasks.size() >= deployProgress.getTargetActiveInstances()) {
        if (shouldCheckLbState(pendingDeploy)) {
          final SingularityLoadBalancerUpdate lbUpdate = lbClient.getState(getLoadBalancerRequestId(pendingDeploy));

          updateLoadBalancerStateForTasks(deployActiveTasks, LoadBalancerRequestType.ADD, lbUpdate);

          DeployState deployState = interpretLoadBalancerState(lbUpdate, pendingDeploy.getCurrentDeployState());

          if (deployState == DeployState.SUCCEEDED) {
            updatePendingDeploy(pendingDeploy, Optional.of(lbUpdate), DeployState.WAITING);
            return markStepFinished(pendingDeploy, deploy, otherActiveTasks, request, updatePendingDeployRequest);
          } else if (deployState == DeployState.WAITING) {
            updatePendingDeploy(pendingDeploy, Optional.of(lbUpdate), deployState);
            maybeUpdatePendingRequest(pendingDeploy, deploy, request, updatePendingDeployRequest, Optional.of(lbUpdate));
            return new SingularityDeployResult(DeployState.WAITING);
          } else {
            updatePendingDeploy(pendingDeploy, Optional.of(lbUpdate), deployState);
            maybeUpdatePendingRequest(pendingDeploy, deploy, request, updatePendingDeployRequest, Optional.of(lbUpdate));
            return fromLbState(deployState, lbUpdate);
          }
        }

        if (isCancelRequestPresent || isDeployOverdue) {
          if (request.isLoadBalanced() && shouldCancelLoadBalancer(pendingDeploy)) {
            return cancelLoadBalancer(pendingDeploy);
          }

          if (isCancelRequestPresent) {
            LOG.info("Canceling a deploy {} due to cancel request {}", pendingDeploy, cancelRequest.get());
            return new SingularityDeployResult(DeployState.CANCELED, String.format("Canceled due to request by %s at %s", cancelRequest.get().getUser(), cancelRequest.get().getTimestamp()));
          }
        }

        if (isWaitingForCurrentLbRequest(pendingDeploy)) {
          return new SingularityDeployResult(DeployState.WAITING, Optional.of("Waiting on load balancer API"), pendingDeploy.getLastLoadBalancerUpdate(), System.currentTimeMillis());
        }

        final DeployHealth deployHealth = deployHealthHelper.getDeployHealth(request, deploy, deployActiveTasks, true);
        switch (deployHealth) {
          case WAITING:
            maybeUpdatePendingRequest(pendingDeploy, deploy, request, updatePendingDeployRequest);
            return checkOverdue(deploy, deployActiveTasks, isDeployOverdue);
          case HEALTHY:
            if (request.isLoadBalanced()) {
              if (updatePendingDeployRequest.isPresent() && updatePendingDeployRequest.get().getTargetActiveInstances() != deployProgress.getTargetActiveInstances()) {
                maybeUpdatePendingRequest(pendingDeploy, deploy, request, updatePendingDeployRequest);
                return new SingularityDeployResult(DeployState.WAITING);
              } else {
                // don't check overdue here because we want to give it a chance to enqueue the load
                // balancer request. the next check will determine its fate.
                if (configuration.getLoadBalancerUri() == null) {
                  LOG.warn("Deploy {} required a load balancer URI but it wasn't set", pendingDeploy);
                  return new SingularityDeployResult(DeployState.FAILED, "No valid load balancer URI was present");
                }

                Collection<SingularityTaskId> toShutDown = tasksToShutDown(deployProgress, otherActiveTasks, request);
                final Map<SingularityTaskId, SingularityTask> tasks = taskManager.getTasks(Iterables.concat(deployActiveTasks, toShutDown));
                final LoadBalancerRequestId lbRequestId = getLoadBalancerRequestId(pendingDeploy);

                updateLoadBalancerStateForTasks(deployActiveTasks, LoadBalancerRequestType.ADD,
                  new SingularityLoadBalancerUpdate(BaragonRequestState.UNKNOWN, lbRequestId, Optional.<String> absent(), System.currentTimeMillis(), LoadBalancerMethod.PRE_ENQUEUE,
                    Optional.<String> absent()));
                SingularityLoadBalancerUpdate enqueueResult = lbClient.enqueue(lbRequestId, request, deploy.get(), getTasks(deployActiveTasks, tasks), getTasks(toShutDown, tasks));
                updateLoadBalancerStateForTasks(deployActiveTasks, LoadBalancerRequestType.ADD, enqueueResult);

                DeployState deployState = interpretLoadBalancerState(enqueueResult, DeployState.WAITING);
                if (deployState == DeployState.SUCCEEDED) {
                  updatePendingDeploy(pendingDeploy, Optional.of(enqueueResult), DeployState.WAITING);
                  return markStepFinished(pendingDeploy, deploy, otherActiveTasks, request, updatePendingDeployRequest);
                } else if (deployState == DeployState.WAITING) {
                  updatePendingDeploy(pendingDeploy, Optional.of(enqueueResult), deployState);
                  return new SingularityDeployResult(DeployState.WAITING);
                } else {
                  return fromLbState(deployState, enqueueResult);
                }
              }
            } else {
              return markStepFinished(pendingDeploy, deploy, otherActiveTasks, request, updatePendingDeployRequest);
            }
          case UNHEALTHY:
          default:
            return new SingularityDeployResult(DeployState.FAILED, "Not all tasks for deploy were healthy");
        }
      } else {
        maybeUpdatePendingRequest(pendingDeploy, deploy, request, updatePendingDeployRequest);
        return checkOverdue(deploy, deployActiveTasks, isDeployOverdue);
      }
    }
  }

  private void maybeUpdatePendingRequest(SingularityPendingDeploy pendingDeploy, Optional<SingularityDeploy> deploy, SingularityRequest request, Optional<SingularityUpdatePendingDeployRequest> updatePendingDeployRequest) {
    maybeUpdatePendingRequest(pendingDeploy, deploy, request, updatePendingDeployRequest, Optional.<SingularityLoadBalancerUpdate> absent());
  }

  private void maybeUpdatePendingRequest(SingularityPendingDeploy pendingDeploy, Optional<SingularityDeploy> deploy, SingularityRequest request, Optional<SingularityUpdatePendingDeployRequest> updatePendingDeployRequest, Optional<SingularityLoadBalancerUpdate> lbUpdate) {
    if (updatePendingDeployRequest.isPresent() && pendingDeploy.getDeployProgress().isPresent()) {
      SingularityDeployProgress deployProgress = pendingDeploy.getDeployProgress().get();
      SingularityDeployProgress newProgress = new SingularityDeployProgress(
        Math.min(updatePendingDeployRequest.get().getTargetActiveInstances(), request.getInstancesSafe()),
        deployProgress.getDeployInstanceCountPerStep(),
        deployProgress.getDeployStepWaitTimeSeconds(),
        false,
        deployProgress.isAutoAdvanceDeploySteps(),
        System.currentTimeMillis()
      );
      updatePendingDeploy(pendingDeploy, lbUpdate.or(pendingDeploy.getLastLoadBalancerUpdate()), DeployState.WAITING, Optional.of(newProgress));
      requestManager.addToPendingQueue(new SingularityPendingRequest(request.getId(), pendingDeploy.getDeployMarker().getDeployId(), System.currentTimeMillis(), pendingDeploy.getDeployMarker().getUser(),
        PendingType.NEXT_DEPLOY_STEP, deploy.isPresent() ? deploy.get().getSkipHealthchecksOnDeploy() : Optional.<Boolean> absent(),
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

  private SingularityDeployResult markStepFinished(SingularityPendingDeploy pendingDeploy, Optional<SingularityDeploy> deploy, Collection<SingularityTaskId> otherActiveTasks, SingularityRequest request, Optional<SingularityUpdatePendingDeployRequest> updatePendingDeployRequest) {
    SingularityDeployProgress deployProgress = pendingDeploy.getDeployProgress().get();
    if (updatePendingDeployRequest.isPresent() && getNewTargetInstances(deployProgress, request, updatePendingDeployRequest) != deployProgress.getTargetActiveInstances()) {
      maybeUpdatePendingRequest(pendingDeploy, deploy, request, updatePendingDeployRequest);
      return new SingularityDeployResult(DeployState.WAITING);
    }
    SingularityDeployProgress newProgress = new SingularityDeployProgress(
        deployProgress.getTargetActiveInstances(),
        deployProgress.getDeployInstanceCountPerStep(),
        deployProgress.getDeployStepWaitTimeSeconds(),
        true,
        deployProgress.isAutoAdvanceDeploySteps(),
        System.currentTimeMillis()
    );
    DeployState deployState;
    if (isLastStepFinished(newProgress, request)) {
      deployState = DeployState.SUCCEEDED;
    } else {
      deployState = DeployState.WAITING;
    }
    updatePendingDeploy(pendingDeploy, pendingDeploy.getLastLoadBalancerUpdate(), deployState, Optional.of(newProgress));
    for (SingularityTaskId taskId : tasksToShutDown(deployProgress, otherActiveTasks, request)) {
      taskManager.createTaskCleanup(new SingularityTaskCleanup(Optional.<String>absent(), TaskCleanupType.DEPLOY_STEP_FINISHED, System.currentTimeMillis(), taskId, Optional.of("Pending deploy step finished"), Optional.<String> absent()));
    }
    return new SingularityDeployResult(deployState);
  }

  private List<SingularityTaskId> tasksToShutDown(SingularityDeployProgress deployProgress, Collection<SingularityTaskId> otherActiveTasks, SingularityRequest request) {
    int numTasksToShutDown = Math.max(otherActiveTasks.size() - (request.getInstancesSafe() - deployProgress.getTargetActiveInstances()), 0);
    List<SingularityTaskId> sortedOtherTasks = new ArrayList<>(otherActiveTasks);
    Collections.sort(sortedOtherTasks, SingularityTaskId.INSTANCE_NO_COMPARATOR);
    return sortedOtherTasks.subList(0, numTasksToShutDown);
  }

  private boolean canMoveToNextStep(SingularityDeployProgress deployProgress) {
    return deployProgress.isAutoAdvanceDeploySteps() && deployProgress.getTimestamp() + TimeUnit.SECONDS.toMillis(deployProgress.getDeployStepWaitTimeSeconds()) < System.currentTimeMillis();
  }

  private int getNewTargetInstances(SingularityDeployProgress deployProgress, SingularityRequest request, Optional<SingularityUpdatePendingDeployRequest> updateRequest) {
    if (updateRequest.isPresent()) {
      return Math.min(updateRequest.get().getTargetActiveInstances(), request.getInstancesSafe());
    } else {
      return Math.min(deployProgress.getTargetActiveInstances() + deployProgress.getDeployInstanceCountPerStep(), request.getInstancesSafe());
    }
  }

  private SingularityDeployResult checkOverdue(Optional<SingularityDeploy> deploy, Collection<SingularityTaskId> deployActiveTasks, boolean isOverdue) {
    String message = null;

    if (deploy.isPresent()) {
      message = String.format("Deploy was able to launch %s tasks, but not all of them became healthy within %s", deployActiveTasks.size(), JavaUtils.durationFromMillis(getAllowedMillis(deploy.get())));
    }

    if (deploy.isPresent() && isOverdue) {
      return new SingularityDeployResult(DeployState.OVERDUE, message);
    } else {
      return new SingularityDeployResult(DeployState.WAITING);
    }
  }

}
