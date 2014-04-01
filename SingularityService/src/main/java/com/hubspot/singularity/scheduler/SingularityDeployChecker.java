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
import com.hubspot.singularity.LoadBalancerState;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityDeployState;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityRequest;
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
    
    final Map<SingularityPendingDeploy, SingularityDeployKey> pendingDeployToKey = SingularityDeployKey.fromPendingDeploys(pendingDeploys);
    final Map<SingularityDeployKey, SingularityDeploy> deployKeyToDeploy = deployManager.getDeploysForKeys(pendingDeployToKey.values());

    final List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();

    for (SingularityPendingDeploy pendingDeploy : pendingDeploys) {
      LOG.debug("Checking a deploy {}", pendingDeploy);
      
      checkDeploy(pendingDeploy, cancelDeploys, pendingDeployToKey, deployKeyToDeploy, activeTaskIds);
    }
    
    for (SingularityDeployMarker cancelDeploy : cancelDeploys) {
      SingularityDeleteResult deleteResult = deployManager.deleteCancelRequest(cancelDeploy);
      
      LOG.debug("Removing cancel deploy request {} - {}", cancelDeploy, deleteResult);
    }
    
    return pendingDeploys.size();
  }
  
  private void checkDeploy(final SingularityPendingDeploy pendingDeploy, final List<SingularityDeployMarker> cancelDeploys, final Map<SingularityPendingDeploy, SingularityDeployKey> pendingDeployToKey, final Map<SingularityDeployKey, SingularityDeploy> deployKeyToDeploy, final List<SingularityTaskId> activeTaskIds) {
    final SingularityDeployKey deployKey = pendingDeployToKey.get(pendingDeploy);
    final SingularityDeploy deploy = deployKeyToDeploy.get(deployKey);
    
    Optional<SingularityRequest> maybeRequest = requestManager.fetchRequest(pendingDeploy.getDeployMarker().getRequestId());

    if (!maybeRequest.isPresent()) {
      LOG.warn("Deploy {} was missing a request, removing deploy", pendingDeploy);
      
      if (shouldCancelLoadBalancer(pendingDeploy)) {
        cancelLoadBalancer(pendingDeploy);
      }
      
      removePendingDeploy(pendingDeploy);
      return;
    }
    
    final SingularityDeployMarker pendingDeployMarker = pendingDeploy.getDeployMarker();

    final Optional<SingularityDeployMarker> cancelRequest = findCancel(cancelDeploys, pendingDeployMarker);
    
    final SingularityRequest request = maybeRequest.get();
    
    final Iterable<SingularityTaskId> requestMatchingActiveTasks = Iterables.filter(activeTaskIds, SingularityTaskId.matchingRequest(pendingDeployMarker.getRequestId()));
    
    final List<SingularityTaskId> deployMatchingTasks = Lists.newArrayList(Iterables.filter(requestMatchingActiveTasks, SingularityTaskId.matchingDeploy(pendingDeployMarker.getDeployId())));
    final List<SingularityTaskId> allOtherMatchingTasks = Lists.newArrayList(Iterables.filter(requestMatchingActiveTasks, Predicates.not(SingularityTaskId.matchingDeploy(pendingDeployMarker.getDeployId()))));
    
    DeployState deployState = getDeployState(request, cancelRequest, pendingDeploy, deployKey, deploy, deployMatchingTasks, allOtherMatchingTasks);

    LOG.info("Deploy {} had state {} after {}", pendingDeployMarker, deployState, DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - pendingDeployMarker.getTimestamp()));
    
    if (deployState == DeployState.SUCCEEDED) {
      if (saveNewDeployState(pendingDeployMarker, Optional.of(pendingDeployMarker))) {
        finishDeploy(pendingDeploy, allOtherMatchingTasks, deployState);
        return;
      } else {
        LOG.warn("Failing deploy {} because it failed to save deploy state", pendingDeployMarker);
        deployState = DeployState.FAILED_INTERNAL_STATE;
      }
    } else if (deployState == DeployState.WAITING) {
      return;
    }
    
    // success case is handled, handle failure cases:
    saveNewDeployState(pendingDeployMarker, Optional.<SingularityDeployMarker> absent());
    finishDeploy(pendingDeploy, deployMatchingTasks, deployState);
  }
  
  private Optional<SingularityDeployMarker> findCancel(List<SingularityDeployMarker> cancelDeploys, SingularityDeployMarker activeDeploy) {
    for (SingularityDeployMarker cancelDeploy : cancelDeploys) {
      if (cancelDeploy.getRequestId().equals(activeDeploy.getRequestId()) && cancelDeploy.getDeployId().equals(activeDeploy.getDeployId())) {
        return Optional.of(cancelDeploy);
      }
    }
    
    return Optional.absent();
  }
    
  private void cleanupTasks(Iterable<SingularityTaskId> tasksToKill, TaskCleanupType cleanupType) {
    final long now = System.currentTimeMillis();
    
    for (SingularityTaskId matchingTask : tasksToKill) {
      taskManager.createCleanupTask(new SingularityTaskCleanup(Optional.<String> absent(), cleanupType, now, matchingTask));
    }
  }
  
  private boolean saveNewDeployState(SingularityDeployMarker pendingDeployMarker, Optional<SingularityDeployMarker> newActiveDeploy) {
    Optional<SingularityDeployState> deployState = deployManager.getDeployState(pendingDeployMarker.getRequestId());
    
    boolean persistSuccess = false;
    
    if (!deployState.isPresent()) {
      LOG.error("Expected deploy state for deploy marker: {} but didn't find it", pendingDeployMarker);
    } else {
      ConditionalPersistResult deployStatePersistResult = deployManager.saveNewDeployState(new SingularityDeployState(deployState.get().getRequestId(), newActiveDeploy.or(deployState.get().getActiveDeploy()), Optional.<SingularityDeployMarker> absent()), Optional.<Stat> absent(), false);
      
      if (deployStatePersistResult == ConditionalPersistResult.SAVED) {
        persistSuccess = true;
      } else {
        LOG.error("Expected deploy save state {} for deploy marker: {} but instead got {}", ConditionalPersistResult.SAVED, pendingDeployMarker, deployStatePersistResult);
      }
    }
    
    return persistSuccess;
  }
  
  // TODO history this?
  private void finishDeploy(SingularityPendingDeploy pendingDeploy, Iterable<SingularityTaskId> tasksToKill, DeployState deployState) {
    cleanupTasks(tasksToKill, deployState.cleanupType);
    
    removePendingDeploy(pendingDeploy);
  }
  
  private void removePendingDeploy(SingularityPendingDeploy pendingDeploy) {
    deployManager.deletePendingDeploy(pendingDeploy);
  }
  
  private boolean isDeployOverdue(SingularityPendingDeploy pendingDeploy, SingularityDeploy deploy) {
    final long startTime = pendingDeploy.getDeployMarker().getTimestamp();
    
    final long deployDuration = System.currentTimeMillis() - startTime;

    final long allowedTime = TimeUnit.SECONDS.toMillis(deploy.getHealthcheckIntervalSeconds().or(0L) + deploy.getDeployHealthTimeoutSeconds().or(configuration.getDeployHealthyBySeconds()));
    
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
  
  private DeployState enqueSwitchLoadBalancer(SingularityPendingDeploy pendingDeploy, Collection<SingularityTaskId> deployTasks, Collection<SingularityTaskId> allOtherTasks) {
    if (!lbClient.hasValidUri()) {
      LOG.warn("Deploy {} required a load balancer URI but it wasn't set", pendingDeploy);
      return DeployState.FAILED;
    }
    
    final Map<SingularityTaskId, SingularityTask> tasks = taskManager.getTasks(Iterables.concat(deployTasks, allOtherTasks));
    
    Optional<LoadBalancerState> enqueueResult = lbClient.enqueue(pendingDeploy.getDeployMarker().getLoadBalancerRequestId(), getTasks(deployTasks, tasks), getTasks(allOtherTasks, tasks));
    
    Optional<DeployState> deployState = interpretLoadBalancerState(enqueueResult);
    
    if (deployState.isPresent()) {
      return deployState.get();
    }
    
    updatePendingDeploy(new SingularityPendingDeploy(pendingDeploy.getDeployMarker(), enqueueResult));
  
    return DeployState.WAITING;
  }
  
  private void updatePendingDeploy(SingularityPendingDeploy pendingDeploy) {
    deployManager.savePendingDeploy(pendingDeploy);
  }
  
  private Optional<DeployState> interpretLoadBalancerState(Optional<LoadBalancerState> lbState) {
    if (!lbState.isPresent()) {
      return Optional.of(DeployState.WAITING);
    }
    
    switch (lbState.get()) {
    case CANCELED:
      return Optional.of(DeployState.CANCELED);
    case SUCCESS:
      return Optional.of(DeployState.SUCCEEDED);
    case FAILED:
      return Optional.of(DeployState.FAILED);
    case CANCELING:
    case WAITING:
    default:
      return Optional.absent();
    }
  }
  
  private DeployState cancelLoadBalancer(SingularityPendingDeploy pendingDeploy) {
    final Optional<LoadBalancerState> lbState = lbClient.cancel(pendingDeploy.getDeployMarker().getLoadBalancerRequestId());
    
    if (lbState.isPresent()) {
      updatePendingDeploy(new SingularityPendingDeploy(pendingDeploy.getDeployMarker(), lbState));
    }
    
    final Optional<DeployState> deployState = interpretLoadBalancerState(lbState);
        
    return deployState.or(DeployState.WAITING);
  }
  
  private boolean shouldCancelLoadBalancer(final SingularityPendingDeploy pendingDeploy) {
    return pendingDeploy.getLoadBalancerState().isPresent() && pendingDeploy.getLoadBalancerState().get() == LoadBalancerState.WAITING;
  }
  
  private DeployState getDeployState(final SingularityRequest request, final Optional<SingularityDeployMarker> cancelRequest, final SingularityPendingDeploy pendingDeploy, final SingularityDeployKey deployKey, final SingularityDeploy deploy, final Collection<SingularityTaskId> matchingActiveTasks, final Collection<SingularityTaskId> allOtherTasks) {
    if (!request.isDeployable()) {
      LOG.info("Succeeding a deploy {} because the request {} was not deployable", pendingDeploy, request);
      
      return DeployState.SUCCEEDED;
    }
    
    if (pendingDeploy.getLoadBalancerState().isPresent()) {
      final Optional<LoadBalancerState> lbState = lbClient.getState(pendingDeploy.getDeployMarker().getLoadBalancerRequestId());
    
      Optional<DeployState> deployState = interpretLoadBalancerState(lbState);
      
      if (deployState.isPresent()) {
        return deployState.get();
      }
    }
    
    final boolean isCancelRequestPresent = cancelRequest.isPresent();
    final boolean isDeployOverdue = isDeployOverdue(pendingDeploy, deploy);
    
    if (isCancelRequestPresent || isDeployOverdue) {
      if (request.isLoadBalanced()) {
        if (shouldCancelLoadBalancer(pendingDeploy)) {
          return cancelLoadBalancer(pendingDeploy);
        }
        
        return DeployState.WAITING;
      }
      
      if (isCancelRequestPresent) {
        LOG.info("Canceling a deploy {} due to cancel request {}", pendingDeploy, cancelRequest.get());
      }
      
      return DeployState.CANCELED;
    }
    
    if (pendingDeploy.getLoadBalancerState().isPresent()) {
      // we need to check this twice because we need to check overdue + cancels above, at this stage we're waiting.
      return DeployState.WAITING;
    }

    if (matchingActiveTasks.size() < request.getInstancesSafe()) {
      return DeployState.WAITING;
    }
    
    final DeployHealth deployHealth = deployHealthHelper.getDeployHealth(Optional.of(deploy), matchingActiveTasks);
    
    switch (deployHealth) {
    case WAITING:
      return DeployState.WAITING;
    case HEALTHY:
      if (request.isLoadBalanced()) {
        return enqueSwitchLoadBalancer(pendingDeploy, matchingActiveTasks, allOtherTasks);
      } else {
        return DeployState.SUCCEEDED;
      }
    case UNHEALTHY:
    default:
      return DeployState.FAILED;
    }
  }
  
  private enum DeployState {
    SUCCEEDED(TaskCleanupType.NEW_DEPLOY_SUCCEEDED), FAILED_INTERNAL_STATE(TaskCleanupType.DEPLOY_FAILED), WAITING(null), OVERDUE(TaskCleanupType.DEPLOY_FAILED), FAILED(TaskCleanupType.DEPLOY_FAILED), CANCELED(TaskCleanupType.DEPLOY_CANCELED);
    
    private final TaskCleanupType cleanupType;

    private DeployState(TaskCleanupType cleanupType) {
      this.cleanupType = cleanupType;
    }
    
  }
  

}
