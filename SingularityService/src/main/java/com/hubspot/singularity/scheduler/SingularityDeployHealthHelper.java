package com.hubspot.singularity.scheduler;

import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityTaskHealthcheckResult;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskHistoryUpdate.SimplifiedTaskState;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.data.TaskManager;

public class SingularityDeployHealthHelper {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityDeployChecker.class);
  
  private final TaskManager taskManager;
  
  @Inject
  public SingularityDeployHealthHelper(TaskManager taskManager) {
    this.taskManager = taskManager;
  }
  
  public enum DeployHealth {
    WAITING, UNHEALTHY, HEALTHY;
  }
  
  public DeployHealth getDeployHealth(final Optional<SingularityDeploy> deploy, final Collection<SingularityTaskId> activeTasks) {
    if (!deploy.isPresent() || !deploy.get().getHealthcheckUri().isPresent()) {
      return getNoHealthcheckDeployHealth(activeTasks);
    } else {
      return getHealthCheckDeployState(activeTasks); 
    }
  }
  
  private DeployHealth getNoHealthcheckDeployHealth(final Collection<SingularityTaskId> matchingActiveTasks) {
    final Multimap<SingularityTaskId, SingularityTaskHistoryUpdate> taskUpdates = taskManager.getTaskHistoryUpdates(matchingActiveTasks);
    
    for (SingularityTaskId taskId : matchingActiveTasks) {
      Collection<SingularityTaskHistoryUpdate> updates = taskUpdates.get(taskId);
      
      SimplifiedTaskState currentState = SingularityTaskHistoryUpdate.getCurrentState(updates);
      
      switch (currentState) {
      case UNKNOWN:
      case WAITING:
        return DeployHealth.WAITING;
      case DONE:
        LOG.warn("Found an active task ({}) in done state: {}}", taskId, updates);
        return DeployHealth.UNHEALTHY;
      case RUNNING:
        // TODO has it been running long enough?
      }
    }
    
    return DeployHealth.HEALTHY;
  }
  
  private DeployHealth getHealthCheckDeployState(final Collection<SingularityTaskId> matchingActiveTasks) {
    Map<SingularityTaskId, SingularityTaskHealthcheckResult> healthcheckResults = taskManager.getHealthcheckResults(matchingActiveTasks);

    boolean allHealthy = true;
    
    for (SingularityTaskId taskId : matchingActiveTasks) {
      SingularityTaskHealthcheckResult healthcheckResult = healthcheckResults.get(taskId);
      
      if (healthcheckResult == null) {
        LOG.warn("No health check present for {}", taskId);
        allHealthy = false;
      } else if (healthcheckResult.isFailed()) {
        LOG.info("Found a failed health check: {}", healthcheckResult);
        return DeployHealth.UNHEALTHY;
      }
    }
    
    if (allHealthy) {
      return DeployHealth.HEALTHY;
    } else {
      return DeployHealth.WAITING;
    }
  }

}
