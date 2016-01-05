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
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;
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

  private boolean shouldCheckHealthchecks(final SingularityRequest request, final Optional<SingularityDeploy> deploy, final Collection<SingularityTaskId> activeTasks, final boolean isDeployPending) {
    if (!deploy.isPresent()) {
      return false;
    }

    if (!deploy.get().getHealthcheckUri().isPresent()) {
      return false;
    }

    if (isDeployPending && deploy.get().getSkipHealthchecksOnDeploy().or(false)) {
      return false;
    }

    if (request.getSkipHealthchecks().or(Boolean.FALSE)) {
      return false;
    }

    for (SingularityTask task : taskManager.getTasks(activeTasks).values()) {
      if (task.getTaskRequest().getPendingTask().getSkipHealthchecks().or(Boolean.FALSE)) {
        return false;
      }
    }

    return true;
  }

  public DeployHealth getDeployHealth(final SingularityRequest request, final Optional<SingularityDeploy> deploy, final Collection<SingularityTaskId> activeTasks, final boolean isDeployPending) {
    if (shouldCheckHealthchecks(request, deploy, activeTasks, isDeployPending)) {
      return getHealthcheckDeployState(deploy.get(), activeTasks, isDeployPending);
    } else {
      return getNoHealthcheckDeployHealth(deploy, activeTasks);
    }
  }

  public int getNumHealthyTasks(final Optional<SingularityDeploy> deploy, final Collection<SingularityTaskId> activeTasks, final boolean isDeployPending) {
    if (!deploy.isPresent() || !deploy.get().getHealthcheckUri().isPresent() || (isDeployPending && deploy.get().getSkipHealthchecksOnDeploy().or(false))) {
      return getNumHealthcheckHealthyTasks(deploy, activeTasks);
    } else {
      return getNumHealthcheckedHealthyTasks(deploy.get(), activeTasks, isDeployPending);
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
          if (!isRunningTaskHealthy(deploy, updates, taskId)) {
            return DeployHealth.WAITING;
          }
      }
    }

    return DeployHealth.HEALTHY;
  }

  private int getNumHealthcheckHealthyTasks(final Optional<SingularityDeploy> deploy, final Collection<SingularityTaskId> matchingActiveTasks) {
    final Map<SingularityTaskId, List<SingularityTaskHistoryUpdate>> taskUpdates = taskManager.getTaskHistoryUpdates(matchingActiveTasks);

    int healthyCount = 0;
    for (SingularityTaskId taskId : matchingActiveTasks) {
      Collection<SingularityTaskHistoryUpdate> updates = taskUpdates.get(taskId);
      SimplifiedTaskState currentState = SingularityTaskHistoryUpdate.getCurrentState(updates);
      if (currentState == SimplifiedTaskState.RUNNING && isRunningTaskHealthy(deploy, updates, taskId)) {
        healthyCount++;
      }
    }
    return healthyCount;
  }

  private boolean isRunningTaskHealthy(final Optional<SingularityDeploy> deploy, Collection<SingularityTaskHistoryUpdate> updates, SingularityTaskId taskId) {
    long runningThreshold = configuration.getConsiderTaskHealthyAfterRunningForSeconds();
    if (deploy.isPresent()) {
      runningThreshold = deploy.get().getConsiderHealthyAfterRunningForSeconds().or(runningThreshold);
    }

    if (runningThreshold < 1) {
      return true;
    }

    Optional<SingularityTaskHistoryUpdate> runningUpdate = SingularityTaskHistoryUpdate.getUpdate(updates, ExtendedTaskState.TASK_RUNNING);
    long taskDuration = System.currentTimeMillis() - runningUpdate.get().getTimestamp();

    if (taskDuration < TimeUnit.SECONDS.toMillis(runningThreshold)) {
      LOG.debug("Task {} has been running for {}, has not yet reached running threshold of {}", taskId, JavaUtils.durationFromMillis(taskDuration), JavaUtils.durationFromMillis(runningThreshold));
      return false;
    }
    return true;
  }

  private DeployHealth getHealthcheckDeployState(final SingularityDeploy deploy, final Collection<SingularityTaskId> matchingActiveTasks, final boolean isDeployPending) {
    Map<SingularityTaskId, SingularityTaskHealthcheckResult> healthcheckResults = taskManager.getLastHealthcheck(matchingActiveTasks);

    for (SingularityTaskId taskId : matchingActiveTasks) {
      DeployHealth individualTaskHealth = getTaskHealth(deploy, isDeployPending, healthcheckResults, taskId);
      if (individualTaskHealth != DeployHealth.HEALTHY) {
        return individualTaskHealth;
      }
    }
    return DeployHealth.HEALTHY;
  }

  private int getNumHealthcheckedHealthyTasks(final SingularityDeploy deploy, final Collection<SingularityTaskId> matchingActiveTasks, final boolean isDeployPending) {
    Map<SingularityTaskId, SingularityTaskHealthcheckResult> healthcheckResults = taskManager.getLastHealthcheck(matchingActiveTasks);

    int healthyCount = 0;
    for (SingularityTaskId taskId : matchingActiveTasks) {
      DeployHealth individualTaskHealth = getTaskHealth(deploy, isDeployPending, healthcheckResults, taskId);
      if (individualTaskHealth == DeployHealth.HEALTHY) {
        healthyCount++;
      }
    }
    return healthyCount;
  }

  private DeployHealth getTaskHealth(SingularityDeploy deploy, boolean isDeployPending, Map<SingularityTaskId, SingularityTaskHealthcheckResult> healthcheckResults, SingularityTaskId taskId) {
    SingularityTaskHealthcheckResult healthcheckResult = healthcheckResults.get(taskId);

    if (healthcheckResult == null) {
      LOG.debug("No healthcheck present for {}", taskId);
      return DeployHealth.WAITING;
    } else if (healthcheckResult.isFailed()) {
      LOG.debug("Found a failed healthcheck: {}", healthcheckResult);

      final Optional<Integer> healthcheckMaxRetries = deploy.getHealthcheckMaxRetries().or(configuration.getHealthcheckMaxRetries());

      if (healthcheckMaxRetries.isPresent() && taskManager.getNumHealthchecks(taskId) > healthcheckMaxRetries.get()) {
        LOG.debug("{} failed {} healthchecks, the max for the deploy", taskId, healthcheckMaxRetries.get());
        return DeployHealth.UNHEALTHY;
      }

      final Optional<Long> healthcheckMaxTotalTimeoutSeconds = deploy.getHealthcheckMaxTotalTimeoutSeconds().or(configuration.getHealthcheckMaxTotalTimeoutSeconds());

      if (isDeployPending && healthcheckMaxTotalTimeoutSeconds.isPresent()) {
        Collection<SingularityTaskHistoryUpdate> updates = taskManager.getTaskHistoryUpdates(taskId);

        long runningAt = 0;

        for (SingularityTaskHistoryUpdate update : updates) {
          if (update.getTaskState() == ExtendedTaskState.TASK_RUNNING) {
            runningAt = update.getTimestamp();
            break;
          }
        }

        if (runningAt > 0) {
          final long durationSinceRunning = System.currentTimeMillis() - runningAt;

          if (durationSinceRunning > TimeUnit.SECONDS.toMillis(healthcheckMaxTotalTimeoutSeconds.get())) {
            LOG.debug("{} has been running for {} and has yet to pass healthchecks, failing deploy", taskId, JavaUtils.durationFromMillis(durationSinceRunning));

            return DeployHealth.UNHEALTHY;
          }
        }
      }

      return DeployHealth.WAITING;
    }
    return DeployHealth.HEALTHY;
  }
}
