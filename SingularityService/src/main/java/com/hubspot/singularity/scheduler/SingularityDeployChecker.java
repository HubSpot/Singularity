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
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityDeployState;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskCleanup.TaskCleanupType;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.DeployManager.ConditionalPersistResult;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.scheduler.SingularityDeployHealthHelper.DeployHealth;

public class SingularityDeployChecker {
  
  private final static Logger LOG = LoggerFactory.getLogger(SingularityDeployChecker.class);

  private final DeployManager deployManager;
  private final TaskManager taskManager;
  private final SingularityDeployHealthHelper deployHealthHelper;
  private final RequestManager requestManager;
  private final SingularityConfiguration configuration;
  
  @Inject
  public SingularityDeployChecker(DeployManager deployManager, SingularityDeployHealthHelper deployHealthHelper, RequestManager requestManager, TaskManager taskManager, SingularityConfiguration configuration) {
    this.configuration = configuration;
    this.deployHealthHelper = deployHealthHelper;
    this.requestManager = requestManager;
    this.deployManager = deployManager;
    this.taskManager = taskManager;
  }
  
  public int checkDeploys() {
    final List<SingularityDeployMarker> activeDeploys = deployManager.getActiveDeploys();
    final List<SingularityDeployMarker> cancelDeploys = deployManager.getCancelDeploys();
    
    final Map<SingularityDeployMarker, SingularityDeployKey> markerToKey = SingularityDeployKey.fromDeployMarkers(activeDeploys);
    final Map<SingularityDeployKey, SingularityDeploy> deployKeyToDeploy = deployManager.getDeploysForKeys(markerToKey.values());

    final List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();

    for (SingularityDeployMarker activeDeployMarker : activeDeploys) {
      LOG.debug("Checking a deploy {}", activeDeployMarker);
      
      checkDeploy(activeDeployMarker, cancelDeploys, markerToKey, deployKeyToDeploy, activeTaskIds);
    }
    
    for (SingularityDeployMarker cancelDeploy : cancelDeploys) {
      SingularityDeleteResult deleteResult = deployManager.deleteCancelRequest(cancelDeploy);
      
      LOG.debug("Removing cancel deploy request {} - {}", cancelDeploy, deleteResult);
    }
    
    return activeDeploys.size();
  }
  
  private void checkDeploy(final SingularityDeployMarker activeDeployMarker, final List<SingularityDeployMarker> cancelDeploys, final Map<SingularityDeployMarker, SingularityDeployKey> markerToKey, final Map<SingularityDeployKey, SingularityDeploy> deployKeyToDeploy, final List<SingularityTaskId> activeTaskIds) {
    Optional<SingularityRequest> maybeRequest = requestManager.fetchRequest(activeDeployMarker.getRequestId());
    
    if (!maybeRequest.isPresent()) {
      LOG.warn("Deploy {} was missing a request, removing deploy", activeDeployMarker);
      removeActiveDeployMarker(activeDeployMarker);
      return;
    }

    final Optional<SingularityDeployMarker> cancelRequest = findCancel(cancelDeploys, activeDeployMarker);
    
    final SingularityRequest request = maybeRequest.get();
    
    final SingularityDeployKey deployKey = markerToKey.get(activeDeployMarker);
    final SingularityDeploy deploy = deployKeyToDeploy.get(deployKey);

    final Iterable<SingularityTaskId> requestMatchingActiveTasks = Iterables.filter(activeTaskIds, SingularityTaskId.matchingRequest(activeDeployMarker.getRequestId()));
    
    final List<SingularityTaskId> deployMatchingTasks = Lists.newArrayList(Iterables.filter(requestMatchingActiveTasks, SingularityTaskId.matchingDeploy(activeDeployMarker.getDeployId())));

    DeployState deployState = getDeployState(request, cancelRequest, activeDeployMarker, deployKey, deploy, deployMatchingTasks);

    LOG.info("Deploy {} had state {} after {}", activeDeployMarker, deployState, DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - activeDeployMarker.getTimestamp()));
    
    if (deployState == DeployState.SUCCEEDED) {
      if (saveNewDeployState(activeDeployMarker, Optional.of(activeDeployMarker))) {
        finishDeploy(activeDeployMarker, Iterables.filter(requestMatchingActiveTasks, Predicates.not(SingularityTaskId.matchingDeploy(activeDeployMarker.getDeployId()))), deployState);
        return;
      } else {
        LOG.warn("Failing deploy {} because it failed to save deploy state", activeDeployMarker);
        deployState = DeployState.FAILED_INTERNAL_STATE;
      }
    } else if (deployState == DeployState.WAITING) {
      return;
    }
    
    // success case is handled, handle failure cases:
    finishDeploy(activeDeployMarker, deployMatchingTasks, deployState);
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
  
  private boolean saveNewDeployState(SingularityDeployMarker activeDeployMarker, Optional<SingularityDeployMarker> newActiveDeploy) {
    Optional<SingularityDeployState> deployState = deployManager.getDeployState(activeDeployMarker.getRequestId());
    
    boolean persistSuccess = false;
    
    if (!deployState.isPresent()) {
      LOG.error("Expected deploy state for deploy marker: {} but didn't find it", activeDeployMarker);
    } else {
      ConditionalPersistResult deployStatePersistResult = deployManager.saveNewDeployState(new SingularityDeployState(deployState.get().getRequestId(), newActiveDeploy.or(deployState.get().getActiveDeploy()), Optional.<SingularityDeployMarker> absent()), Optional.<Stat> absent(), false);
      
      if (deployStatePersistResult == ConditionalPersistResult.SAVED) {
        persistSuccess = true;
      } else {
        LOG.error("Expected deploy save state {} for deploy marker: {} but instead got {}", ConditionalPersistResult.SAVED, activeDeployMarker, deployStatePersistResult);
      }
    }
    
    return persistSuccess;
  }
  
  // TODO history this?
  private void finishDeploy(SingularityDeployMarker activeDeployMarker, Iterable<SingularityTaskId> tasksToKill, DeployState deployState) {
    cleanupTasks(tasksToKill, deployState.cleanupType);
    
    removeActiveDeployMarker(activeDeployMarker);
  }
  
  private void removeActiveDeployMarker(SingularityDeployMarker activeDeployMarker) {
    deployManager.deleteActiveDeploy(activeDeployMarker);
  }
  
  private boolean isDeployOverdue(SingularityDeployMarker activeDeployMarker, SingularityDeploy deploy) {
    final long startTime = activeDeployMarker.getTimestamp();
    
    final long deployDuration = System.currentTimeMillis() - startTime;

    final long allowedTime = TimeUnit.SECONDS.toMillis(deploy.getHealthcheckIntervalSeconds().or(0L) + deploy.getDeployHealthTimeoutSeconds().or(configuration.getDeployHealthyBySeconds()));
    
    if (deployDuration > allowedTime) {
      LOG.error("Deploy {} is overdue and will be failed (duration: {}), allowed: {}", activeDeployMarker, DurationFormatUtils.formatDurationHMS(deployDuration), DurationFormatUtils.formatDurationHMS(allowedTime));
      
      return true;
    } else {
      LOG.trace("Deploy {} is not yet overdue (duration: {}), allowed: {}", activeDeployMarker, DurationFormatUtils.formatDurationHMS(deployDuration), DurationFormatUtils.formatDurationHMS(allowedTime));
      
      return false;
    }
  }
  
  private DeployState getDeployState(final SingularityRequest request, final Optional<SingularityDeployMarker> cancelRequest, final SingularityDeployMarker activeDeployMarker, final SingularityDeployKey deployKey, final SingularityDeploy deploy, final Collection<SingularityTaskId> matchingActiveTasks) {
    if (cancelRequest.isPresent()) {
      LOG.info("Canceling a deploy {} due to cancel request {}", activeDeployMarker, cancelRequest.get());
      
      return DeployState.CANCELED;
    }
    
    if (!request.isDeployable()) {
      LOG.info("Succeeding a deploy {} because the request {} was not deployable", activeDeployMarker, request);
      
      return DeployState.SUCCEEDED;
    }
    
    if (matchingActiveTasks.size() < request.getInstancesSafe()) {
      if (isDeployOverdue(activeDeployMarker, deploy)) {
        return DeployState.OVERDUE;
      }
      
      return DeployState.WAITING;
    }
    
    final DeployHealth deployHealth = deployHealthHelper.getDeployHealth(Optional.of(deploy), matchingActiveTasks);
    
    switch (deployHealth) {
    case WAITING:
      return DeployState.WAITING;
    case HEALTHY:
      return DeployState.SUCCEEDED;
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
