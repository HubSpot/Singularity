package com.hubspot.singularity.scheduler;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.LoadBalancerRequestType;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.LoadBalancerState;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityDeployResult;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskCleanup.TaskCleanupType;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.DeployManager.ConditionalPersistResult;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.hooks.LoadBalancerClient;
import com.hubspot.singularity.scheduler.SingularityDeployHealthHelper.DeployHealth;

public class SingularityDeployChecker {
  
  private final static Logger LOG = LoggerFactory.getLogger(SingularityDeployChecker.class);

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
    
    if (pendingDeploys.isEmpty() && cancelDeploys.isEmpty()) {
      return 0;
    }
    
    final Map<SingularityPendingDeploy, SingularityDeployKey> pendingDeployToKey = SingularityDeployKey.fromPendingDeploys(pendingDeploys);
    final Map<SingularityDeployKey, SingularityDeploy> deployKeyToDeploy = deployManager.getDeploysForKeys(pendingDeployToKey.values());

    for (SingularityPendingDeploy pendingDeploy : pendingDeploys) {
      LOG.debug("Checking a deploy {}", pendingDeploy);
      
      checkDeploy(pendingDeploy, cancelDeploys, pendingDeployToKey, deployKeyToDeploy);
    }
    
    for (SingularityDeployMarker cancelDeploy : cancelDeploys) {
      SingularityDeleteResult deleteResult = deployManager.deleteCancelRequest(cancelDeploy);
      
      LOG.debug("Removing cancel deploy request {} - {}", cancelDeploy, deleteResult);
    }
    
    return pendingDeploys.size();
  }
  
  private void checkDeploy(final SingularityPendingDeploy pendingDeploy, final List<SingularityDeployMarker> cancelDeploys, final Map<SingularityPendingDeploy, SingularityDeployKey> pendingDeployToKey, final Map<SingularityDeployKey, SingularityDeploy> deployKeyToDeploy) {
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
    
    final SingularityRequest request = maybeRequestWithState.get().getRequest();
    
    final List<SingularityTaskId> requestTasks = taskManager.getTaskIdsForRequest(request.getId());
    final List<SingularityTaskId> activeTasks = taskManager.filterActiveTaskIds(requestTasks);    
    
    final List<SingularityTaskId> inactiveDeployMatchingTasks = SingularityTaskId.matchingAndNotIn(requestTasks, pendingDeployMarker.getRequestId(), pendingDeployMarker.getDeployId(), activeTasks);
    final List<SingularityTaskId> deployMatchingTasks = Lists.newArrayList(Iterables.filter(activeTasks, SingularityTaskId.matchingDeploy(pendingDeployMarker.getDeployId())));
    final List<SingularityTaskId> allOtherMatchingTasks = Lists.newArrayList(Iterables.filter(activeTasks, Predicates.not(SingularityTaskId.matchingDeploy(pendingDeployMarker.getDeployId()))));
    
    SingularityDeployResult deployResult = getDeployResult(request, cancelRequest, pendingDeploy, deployKey, deploy, deployMatchingTasks, allOtherMatchingTasks, inactiveDeployMatchingTasks);

    LOG.info("Deploy {} had result {} after {}", pendingDeployMarker, deployResult, JavaUtils.durationFromMillis(System.currentTimeMillis() - pendingDeployMarker.getTimestamp()));
    
    if (deployResult.getDeployState() == DeployState.SUCCEEDED) {
      if (saveNewDeployState(pendingDeployMarker, Optional.of(pendingDeployMarker))) {
        if (request.isLoadBalanced()) {
          updateLoadBalancerStateForTasks(deployMatchingTasks, LoadBalancerRequestType.ADD, deployResult.getLbUpdate().get());
          updateLoadBalancerStateForTasks(allOtherMatchingTasks, LoadBalancerRequestType.REMOVE, deployResult.getLbUpdate().get());
        }
        
        finishDeploy(pendingDeploy, allOtherMatchingTasks, deployResult);
        return;
      } else {
        LOG.warn("Failing deploy {} because it failed to save deploy state", pendingDeployMarker);
        deployResult = new SingularityDeployResult(DeployState.FAILED_INTERNAL_STATE, String.format("Deploy had state %s but failed to persist it correctly", deployResult.getDeployState(), deployResult.getLbUpdate(), deployResult.getTimestamp()));
      }
    } else if (!deployResult.getDeployState().isDeployFinished()) {
      return;
    }
    
    // success case is handled, handle failure cases:
    saveNewDeployState(pendingDeployMarker, Optional.<SingularityDeployMarker> absent());
    finishDeploy(pendingDeploy, deployMatchingTasks, deployResult);
  }
  
  private Optional<SingularityDeployMarker> findCancel(List<SingularityDeployMarker> cancelDeploys, SingularityDeployMarker activeDeploy) {
    for (SingularityDeployMarker cancelDeploy : cancelDeploys) {
      if (cancelDeploy.getRequestId().equals(activeDeploy.getRequestId()) && cancelDeploy.getDeployId().equals(activeDeploy.getDeployId())) {
        return Optional.of(cancelDeploy);
      }
    }
    
    return Optional.absent();
  }
  
  private void updateLoadBalancerStateForTasks(List<SingularityTaskId> taskIds, LoadBalancerRequestType type, SingularityLoadBalancerUpdate update) {
    for (SingularityTaskId taskId : taskIds) {
      taskManager.saveLoadBalancerState(taskId, type, update);
    }
  }
  
  private void cleanupTasks(Iterable<SingularityTaskId> tasksToKill, TaskCleanupType cleanupType, long timestamp) {
    for (SingularityTaskId matchingTask : tasksToKill) {
      taskManager.createCleanupTask(new SingularityTaskCleanup(Optional.<String> absent(), cleanupType, timestamp, matchingTask));
    }
  }
  
  private boolean saveNewDeployState(SingularityDeployMarker pendingDeployMarker, Optional<SingularityDeployMarker> newActiveDeploy) {
    Optional<SingularityRequestDeployState> deployState = deployManager.getRequestDeployState(pendingDeployMarker.getRequestId());
    
    boolean persistSuccess = false;
    
    if (!deployState.isPresent()) {
      LOG.error("Expected deploy state for deploy marker: {} but didn't find it", pendingDeployMarker);
    } else {
      ConditionalPersistResult deployStatePersistResult = deployManager.saveNewRequestDeployState(new SingularityRequestDeployState(deployState.get().getRequestId(), newActiveDeploy.or(deployState.get().getActiveDeploy()), Optional.<SingularityDeployMarker> absent()), Optional.<Stat> absent(), false);
      
      if (deployStatePersistResult == ConditionalPersistResult.SAVED) {
        persistSuccess = true;
      } else {
        LOG.error("Expected deploy save state {} for deploy marker: {} but instead got {}", ConditionalPersistResult.SAVED, pendingDeployMarker, deployStatePersistResult);
      }
    }
    
    return persistSuccess;
  }
  
  private void finishDeploy(SingularityPendingDeploy pendingDeploy, Iterable<SingularityTaskId> tasksToKill, SingularityDeployResult deployResult) {
    cleanupTasks(tasksToKill, deployResult.getDeployState().getCleanupType(), deployResult.getTimestamp());
  
    deployManager.saveDeployResult(pendingDeploy.getDeployMarker(), deployResult);
    
    removePendingDeploy(pendingDeploy);
  }
  
  private void removePendingDeploy(SingularityPendingDeploy pendingDeploy) {
    deployManager.deletePendingDeploy(pendingDeploy.getDeployMarker().getRequestId());
  }

  private final long getAllowedMillis(SingularityDeploy deploy) {
    long seconds = deploy.getDeployHealthTimeoutSeconds().or(configuration.getDeployHealthyBySeconds());
    
    if (deploy.getHealthcheckUri().isPresent()) {
      seconds += deploy.getHealthcheckIntervalSeconds().or(configuration.getHealthcheckIntervalSeconds()) + deploy.getHealthcheckTimeoutSeconds().or(configuration.getHealthcheckTimeoutSeconds());
    }
    
    return TimeUnit.SECONDS.toMillis(seconds);
  }
  
  private boolean isDeployOverdue(SingularityPendingDeploy pendingDeploy, Optional<SingularityDeploy> deploy) {
    if (!deploy.isPresent()) {
      LOG.warn("Can't determine if deploy {} is overdue because it was missing", pendingDeploy);
      
      return false;
    }
    
    final long startTime = pendingDeploy.getDeployMarker().getTimestamp();
    
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
  
  private SingularityDeployResult enqueueSwitchLoadBalancer(SingularityRequest request, SingularityDeploy deploy, SingularityPendingDeploy pendingDeploy, Collection<SingularityTaskId> deployTasks, Collection<SingularityTaskId> allOtherTasks) {
    if (!lbClient.hasValidUri()) {
      LOG.warn("Deploy {} required a load balancer URI but it wasn't set", pendingDeploy);
      return new SingularityDeployResult(DeployState.FAILED, "No valid load balancer URI was present");
    }
    
    final Map<SingularityTaskId, SingularityTask> tasks = taskManager.getTasks(Iterables.concat(deployTasks, allOtherTasks));

    SingularityLoadBalancerUpdate enqueueResult = lbClient.enqueue(getLoadBalancerRequestId(pendingDeploy.getDeployMarker()), request, deploy, getTasks(deployTasks, tasks), getTasks(allOtherTasks, tasks));
    
    DeployState deployState = interpretLoadBalancerState(enqueueResult, DeployState.WAITING);

    updatePendingDeploy(new SingularityPendingDeploy(pendingDeploy.getDeployMarker(), Optional.of(enqueueResult), deployState));
    
    return fromLbState(deployState, enqueueResult);
  }
  
  private void updatePendingDeploy(SingularityPendingDeploy pendingDeploy) {
    deployManager.savePendingDeploy(pendingDeploy);
  }
  
  private DeployState interpretLoadBalancerState(SingularityLoadBalancerUpdate lbUpdate, DeployState unknownState) {
    switch (lbUpdate.getLoadBalancerState()) {
    case CANCELED:
      return DeployState.CANCELED;
    case SUCCESS:
      return DeployState.SUCCEEDED;
    case FAILED:
      return DeployState.FAILED;
    case CANCELING:
      return DeployState.CANCELING;
    case UNKNOWN:
      return unknownState;
    case WAITING:
    }
    
    return DeployState.WAITING;
  }
  
  private SingularityDeployResult cancelLoadBalancer(SingularityPendingDeploy pendingDeploy) {
    final SingularityLoadBalancerUpdate lbUpdate = lbClient.cancel(getLoadBalancerRequestId(pendingDeploy.getDeployMarker()));
    
    final DeployState deployState = interpretLoadBalancerState(lbUpdate, DeployState.CANCELING);
    
    updatePendingDeploy(new SingularityPendingDeploy(pendingDeploy.getDeployMarker(), Optional.of(lbUpdate), deployState));
    
    return fromLbState(deployState, lbUpdate);
  }
  
  private boolean shouldCancelLoadBalancer(final SingularityPendingDeploy pendingDeploy) {
    return pendingDeploy.getLastLoadBalancerUpdate().isPresent() && !pendingDeploy.getCurrentDeployState().isDeployFinished();
  }
  
  private SingularityDeployResult fromLbState(DeployState state, SingularityLoadBalancerUpdate lbUpdate) {
    return new SingularityDeployResult(state, lbUpdate);
  }
  
  private boolean shouldCheckLbState(final SingularityPendingDeploy pendingDeploy) {
    return pendingDeploy.getLastLoadBalancerUpdate().isPresent() && pendingDeploy.getLastLoadBalancerUpdate().get().getLoadBalancerState() != LoadBalancerState.UNKNOWN;
  }
  
  private LoadBalancerRequestId getLoadBalancerRequestId(SingularityDeployMarker deployMarker) {
    return new LoadBalancerRequestId(String.format("%s-%s", deployMarker.getRequestId(), deployMarker.getDeployId()), LoadBalancerRequestType.DEPLOY);
  }
  
  private SingularityDeployResult getDeployResult(final SingularityRequest request, final Optional<SingularityDeployMarker> cancelRequest, final SingularityPendingDeploy pendingDeploy, final SingularityDeployKey deployKey, 
      final Optional<SingularityDeploy> deploy, final Collection<SingularityTaskId> deployActiveTasks, final Collection<SingularityTaskId> otherActiveTasks, final Collection<SingularityTaskId> inactiveDeployMatchingTasks) {
    if (!request.isDeployable()) {
      LOG.info("Succeeding a deploy {} because the request {} was not deployable", pendingDeploy, request);
      
      return new SingularityDeployResult(DeployState.SUCCEEDED, "Request not deployable");
    }
    
    if (shouldCheckLbState(pendingDeploy)) {
      final SingularityLoadBalancerUpdate lbUpdate = lbClient.getState(getLoadBalancerRequestId(pendingDeploy.getDeployMarker()));

      DeployState deployState = interpretLoadBalancerState(lbUpdate, pendingDeploy.getCurrentDeployState());
      
      updatePendingDeploy(new SingularityPendingDeploy(pendingDeploy.getDeployMarker(), Optional.of(lbUpdate), deployState));
      
      if (deployState != DeployState.WAITING) {
        return fromLbState(deployState, lbUpdate);
      }
    }
    
    final boolean isCancelRequestPresent = cancelRequest.isPresent();
    final boolean isDeployOverdue = isDeployOverdue(pendingDeploy, deploy);
    
    if (isCancelRequestPresent || isDeployOverdue) {
      if (request.isLoadBalanced()) {
        if (shouldCancelLoadBalancer(pendingDeploy)) {
          return cancelLoadBalancer(pendingDeploy);
        }
      }
      
      if (isCancelRequestPresent) {
        LOG.info("Canceling a deploy {} due to cancel request {}", pendingDeploy, cancelRequest.get());
        return new SingularityDeployResult(DeployState.CANCELED, String.format("Canceled due to request by %s at %s", cancelRequest.get().getUser(), cancelRequest.get().getTimestamp()));
      }
    }
    
    if (pendingDeploy.getLastLoadBalancerUpdate().isPresent()) {
      return new SingularityDeployResult(DeployState.WAITING, Optional.of("Waiting on load balancer API"), pendingDeploy.getLastLoadBalancerUpdate(), System.currentTimeMillis());
    }
    
    if (!inactiveDeployMatchingTasks.isEmpty()) {
      return new SingularityDeployResult(DeployState.FAILED, "A task for this deploy has already failed");
    }
    
    if (deployActiveTasks.size() < request.getInstancesSafe() || !deploy.isPresent()) {
      return checkOverdue(deploy, isDeployOverdue);
    }
    
    final DeployHealth deployHealth = deployHealthHelper.getDeployHealth(deploy, deployActiveTasks);
    
    switch (deployHealth) {
    case WAITING:
      return checkOverdue(deploy, isDeployOverdue);
    case HEALTHY:
      if (request.isLoadBalanced()) {
        // don't check overdue here because we want to give it a chance to enqueue the load balancer request. the next check will determine its fate.
        return enqueueSwitchLoadBalancer(request, deploy.get(), pendingDeploy, deployActiveTasks, otherActiveTasks);
      } else {
        return new SingularityDeployResult(DeployState.SUCCEEDED);
      }
    case UNHEALTHY:
    }
    
    return new SingularityDeployResult(DeployState.FAILED, "At least one task for this deploy failed");
  }

  private SingularityDeployResult checkOverdue(Optional<SingularityDeploy> deploy, boolean isOverdue) {
    if (deploy.isPresent() && isOverdue) {
      return new SingularityDeployResult(DeployState.OVERDUE, String.format("Deploy did not become healthy after %s", JavaUtils.durationFromMillis(getAllowedMillis(deploy.get()))));
    } else {
      return new SingularityDeployResult(DeployState.WAITING);
    }
  }
  

}
