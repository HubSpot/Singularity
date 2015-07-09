package com.hubspot.singularity.scheduler;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityTaskHealthcheckResult;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskHistoryUpdate.SimplifiedTaskState;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.TaskManager;

@Singleton
public class SingularityDeployHealthHelper {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityDeployHealthHelper.class);

  private final TaskManager taskManager;
  private final SingularityConfiguration configuration;

  @Inject
  public SingularityDeployHealthHelper(TaskManager taskManager, SingularityConfiguration configuration) {
    this.taskManager = taskManager;
    this.configuration = configuration;
  }

  public enum DeployHealth {
    WAITING, UNHEALTHY, HEALTHY;
  }

  public DeployHealth getDeployHealth(final Optional<SingularityDeploy> deploy, final Collection<SingularityTaskId> activeTasks, final boolean isDeployPending) {
    if (!deploy.isPresent() || !deploy.get().getHealthcheckUri().isPresent() || (isDeployPending && deploy.get().getSkipHealthchecksOnDeploy().or(false))) {
      return getNoHealthcheckDeployHealth(deploy, activeTasks);
    } else {
      return getHealthcheckDeployState(deploy.get(), activeTasks);
    }
  }

  private DeployHealth getNoHealthcheckDeployHealth(final Optional<SingularityDeploy> deploy, final Collection<SingularityTaskId> matchingActiveTasks) {
    final Map<SingularityTaskId, List<SingularityTaskHistoryUpdate>> taskUpdates = taskManager.getTaskHistoryUpdates(matchingActiveTasks);

    for (SingularityTaskId taskId : matchingActiveTasks) {
      Collection<SingularityTaskHistoryUpdate> updates = taskUpdates.get(taskId);

      SimplifiedTaskState currentState = SingularityTaskHistoryUpdate.getCurrentState(updates);

      switch (currentState) {
        case UNKNOWN:
        case WAITING:
          return DeployHealth.WAITING;
        case DONE:
          LOG.warn("Unexpectedly found an active task ({}) in done state: {}}", taskId, updates);
          return DeployHealth.UNHEALTHY;
        case RUNNING:
          long runningThreshold = configuration.getConsiderTaskHealthyAfterRunningForSeconds();
          if (deploy.isPresent()) {
            runningThreshold = deploy.get().getConsiderHealthyAfterRunningForSeconds().or(runningThreshold);
          }

          if (runningThreshold < 1) {
            continue;
          }

          Optional<SingularityTaskHistoryUpdate> runningUpdate = SingularityTaskHistoryUpdate.getUpdate(updates, ExtendedTaskState.TASK_RUNNING);
          long taskDuration = System.currentTimeMillis() - runningUpdate.get().getTimestamp();

          if (taskDuration < TimeUnit.SECONDS.toMillis(runningThreshold)) {
            LOG.debug("Task {} has been running for {}, has not yet reached running threshold of {}", taskId, JavaUtils.durationFromMillis(taskDuration), JavaUtils.durationFromMillis(runningThreshold));
            return DeployHealth.WAITING;
          }
      }
    }

    return DeployHealth.HEALTHY;
  }

  private DeployHealth getHealthcheckDeployState(final SingularityDeploy deploy, final Collection<SingularityTaskId> matchingActiveTasks) {
    Map<SingularityTaskId, SingularityTaskHealthcheckResult> healthcheckResults = taskManager.getLastHealthcheck(matchingActiveTasks);

    for (SingularityTaskId taskId : matchingActiveTasks) {
      SingularityTaskHealthcheckResult healthcheckResult = healthcheckResults.get(taskId);

      if (healthcheckResult == null) {
        LOG.debug("No healthcheck present for {}", taskId);
        return DeployHealth.WAITING;
      } else if (healthcheckResult.isFailed()) {
        LOG.debug("Found a failed healthcheck: {}", healthcheckResult);

        if (deploy.getHealthcheckMaxRetries().isPresent() && taskManager.getNumHealthchecks(taskId) > deploy.getHealthcheckMaxRetries().get()) {
          LOG.debug("{} failed {} healthchecks, the max for the deploy", taskId, deploy.getHealthcheckMaxRetries().get());
          return DeployHealth.UNHEALTHY;
        }

        return DeployHealth.WAITING;
      }
    }

    return DeployHealth.HEALTHY;
  }

}
