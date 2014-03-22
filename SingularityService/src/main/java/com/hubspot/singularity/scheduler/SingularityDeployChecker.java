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
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityDeployState;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskCleanup.TaskCleanupType;
import com.hubspot.singularity.SingularityTaskHealthcheckResult;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskHistoryUpdate.SimplifiedTaskState;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.DeployManager.ConditionalPersistResult;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;

public class SingularityDeployChecker {
  
  private final static Logger LOG = LoggerFactory.getLogger(SingularityDeployChecker.class);

  private final DeployManager deployManager;
  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final SingularityConfiguration configuration;
  
  @Inject
  public SingularityDeployChecker(DeployManager deployManager, RequestManager requestManager, TaskManager taskManager, SingularityConfiguration configuration) {
    this.configuration = configuration;
    this.requestManager = requestManager;
    this.deployManager = deployManager;
    this.taskManager = taskManager;
  }
  
  public int checkDeploys() {
    // a deploy is successful if the tasks get to TASK_RUNNING and they pass health checks. if there are no health checks, they simply need to hit TASK_RUNNING
    
    final List<SingularityDeployMarker> activeDeploys = deployManager.getActiveDeploys();
    final Map<SingularityDeployMarker, SingularityDeployKey> markerToKey = SingularityDeployKey.fromDeployMarkers(activeDeploys);
    final Map<SingularityDeployKey, SingularityDeploy> deployKeyToDeploy = deployManager.getDeploysForKeys(markerToKey.values());

    final List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();

    for (SingularityDeployMarker activeDeployMarker : activeDeploys) {
      final long now = System.currentTimeMillis();
      
      LOG.debug(String.format("Checking a deploy %s", activeDeployMarker));
      
      Optional<SingularityRequest> maybeRequest = requestManager.fetchRequest(activeDeployMarker.getRequestId());
      
      if (!maybeRequest.isPresent()) {
        LOG.warn(String.format("Deploy %s was missing a request, removing deploy", activeDeployMarker));
        removeActiveDeployMarker(activeDeployMarker);
        continue;
      }
      
      final SingularityRequest request = maybeRequest.get();
      
      final SingularityDeployKey deployKey = markerToKey.get(activeDeployMarker);
      final SingularityDeploy deploy = deployKeyToDeploy.get(deployKey);

      final Iterable<SingularityTaskId> requestMatchingActiveTasks = Iterables.filter(activeTaskIds, SingularityTaskId.matchingRequest(activeDeployMarker.getRequestId()));
      
      final List<SingularityTaskId> deployMatchingTasks = Lists.newArrayList(Iterables.filter(requestMatchingActiveTasks, SingularityTaskId.matchingDeploy(activeDeployMarker.getDeployId())));
      
      final DeployState deployState = checkDeploy(request, activeDeployMarker, deployKey, deploy, deployMatchingTasks);

      LOG.info(String.format("Deploy %s with state %s after %s", activeDeployMarker, deployState, DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - now)));
      
      switch (deployState) {
      case SUCCEEDED:
        succeedDeploy(activeDeployMarker, Iterables.filter(requestMatchingActiveTasks, Predicates.not(SingularityTaskId.matchingDeploy(activeDeployMarker.getDeployId()))));
        break;
      case WAITING:
        if (!isDeployOverdue(activeDeployMarker, deploy)) {
          break;
        }
      case FAILED:
        failDeploy(activeDeployMarker, deployMatchingTasks);
        break;
      }
    }
    
    return activeDeploys.size();
  }
  
  private void succeedDeploy(SingularityDeployMarker activeDeployMarker, Iterable<SingularityTaskId> otherActiveTasks) {
    finishDeploy(activeDeployMarker, otherActiveTasks, TaskCleanupType.NEW_DEPLOY_SUCCEEDED, Optional.of(activeDeployMarker));
  }
    
  private void finishDeploy(SingularityDeployMarker activeDeployMarker, Iterable<SingularityTaskId> tasksToKill, TaskCleanupType cleanupType, Optional<SingularityDeployMarker> newActiveDeploy) {
    final long now = System.currentTimeMillis();
    
    for (SingularityTaskId matchingTask : tasksToKill) {
      taskManager.createCleanupTask(new SingularityTaskCleanup(Optional.<String> absent(), cleanupType, now, matchingTask));
    }
    
    Optional<SingularityDeployState> deployState = deployManager.getDeployState(activeDeployMarker.getRequestId());
    
    if (!deployState.isPresent()) {
      LOG.error(String.format("Expected deploy state for deploy marker: %s but didn't find it", activeDeployMarker));
    } else {
      ConditionalPersistResult deployStatePersistResult = deployManager.saveNewDeployState(new SingularityDeployState(deployState.get().getRequestId(), newActiveDeploy.or(deployState.get().getActiveDeploy()), Optional.<SingularityDeployMarker> absent()), Optional.<Stat> absent(), false);
      
      if (deployStatePersistResult != ConditionalPersistResult.SAVED) {
        LOG.error(String.format("Expected deploy save state %s for deploy marker: %s but instead got %s", ConditionalPersistResult.SAVED, activeDeployMarker, deployStatePersistResult));
      }
    }
    
    removeActiveDeployMarker(activeDeployMarker);
  }
  
  private void removeActiveDeployMarker(SingularityDeployMarker activeDeployMarker) {
    deployManager.deleteActiveDeploy(activeDeployMarker);
  }
    
  private void failDeploy(SingularityDeployMarker activeDeployMarker, List<SingularityTaskId> matchingActiveTasks) {
    finishDeploy(activeDeployMarker, matchingActiveTasks, TaskCleanupType.DEPLOY_FAILED, Optional.<SingularityDeployMarker> absent());
  }
  
  private boolean isDeployOverdue(SingularityDeployMarker activeDeployMarker, SingularityDeploy deploy) {
    final long startTime = activeDeployMarker.getTimestamp();
    
    final long deployDuration = System.currentTimeMillis() - startTime;

    final long allowedTime = TimeUnit.SECONDS.toMillis(deploy.getHealthcheckIntervalSeconds().or(0L) + deploy.getDeployHealthTimeoutSeconds().or(configuration.getDeployHealthyBySeconds()));
    
    if (deployDuration > allowedTime) {
      LOG.error(String.format("Deploy %s is overdue and will be failed (duration: %s), allowed: %s", activeDeployMarker, DurationFormatUtils.formatDurationHMS(deployDuration), DurationFormatUtils.formatDurationHMS(allowedTime)));
      
      return true;
    } else {
      LOG.trace(String.format("Deploy %s is not yet overdue (duration: %s), allowed: %s", activeDeployMarker, DurationFormatUtils.formatDurationHMS(deployDuration), DurationFormatUtils.formatDurationHMS(allowedTime)));
      
      return false;
    }
  }
  
  private DeployState checkDeploy(final SingularityRequest request, final SingularityDeployMarker activeDeployMarker, final SingularityDeployKey deployKey, final SingularityDeploy deploy, final Collection<SingularityTaskId> matchingActiveTasks) {
    if (matchingActiveTasks.size() < request.getInstancesSafe()) {
      return DeployState.WAITING;
    }
    
    if (!deploy.getHealthcheckUri().isPresent()) {
      return getNoHealthcheckDeployState(matchingActiveTasks, activeDeployMarker);
    } else {
      return getHealthCheckDeployState(matchingActiveTasks, activeDeployMarker); 
    }
  }
  
  private enum DeployState {
    SUCCEEDED, WAITING, FAILED;
  }
  
  private DeployState getNoHealthcheckDeployState(final Collection<SingularityTaskId> matchingActiveTasks, final SingularityDeployMarker activeDeployMarker) {
    final Multimap<SingularityTaskId, SingularityTaskHistoryUpdate> taskUpdates = taskManager.getTaskHistoryUpdates(matchingActiveTasks);
    
    for (SingularityTaskId taskId : matchingActiveTasks) {
      Collection<SingularityTaskHistoryUpdate> updates = taskUpdates.get(taskId);
      
      SimplifiedTaskState currentState = SingularityTaskHistoryUpdate.getCurrentState(updates);
      
      switch (currentState) {
      case UNKNOWN:
      case WAITING:
        return DeployState.WAITING;
      case DONE:
        LOG.warn(String.format("Found an active task (%s) in done state: %s for deploy: %s", taskId, updates, activeDeployMarker));
        return DeployState.FAILED;
      case RUNNING:
      }
    }
    
    return DeployState.SUCCEEDED;
  }
  
  private DeployState getHealthCheckDeployState(final Collection<SingularityTaskId> matchingActiveTasks, final SingularityDeployMarker activeDeployMarker) {
    Map<SingularityTaskId, SingularityTaskHealthcheckResult> healthcheckResults = taskManager.getHealthcheckResults(matchingActiveTasks);

    boolean allHealthy = true;
    
    for (SingularityTaskId taskId : matchingActiveTasks) {
      SingularityTaskHealthcheckResult healthcheckResult = healthcheckResults.get(taskId);
      
      if (healthcheckResult == null) {
        LOG.warn(String.format("No health check present for %s", taskId));
        allHealthy = false;
      } else if (healthcheckResult.getErrorMessage().isPresent()) {
        LOG.info(String.format("Failing deploy %s due to failed health check: %s", activeDeployMarker, healthcheckResult));
        return DeployState.FAILED;
      }
    }
    
    if (allHealthy) {
      return DeployState.SUCCEEDED;
    } else {
      return DeployState.WAITING;
    }
  }
  
}
