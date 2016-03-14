package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployFailure;
import com.hubspot.singularity.SingularityDeployFailureReason;
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

  public List<SingularityTaskId> getHealthyTasks(final SingularityRequest request, final Optional<SingularityDeploy> deploy, final Collection<SingularityTaskId> activeTasks, final boolean isDeployPending) {
    if (shouldCheckHealthchecks(request, deploy, activeTasks, isDeployPending)) {
      return getHealthcheckedHealthyTasks(deploy.get(), activeTasks, isDeployPending);
    } else {
      return getNoHealthcheckHealthyTasks(deploy, activeTasks);
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

  private List<SingularityTaskId> getNoHealthcheckHealthyTasks(final Optional<SingularityDeploy> deploy, final Collection<SingularityTaskId> matchingActiveTasks) {
    final Map<SingularityTaskId, List<SingularityTaskHistoryUpdate>> taskUpdates = taskManager.getTaskHistoryUpdates(matchingActiveTasks);
    final List<SingularityTaskId> healthyTaskIds = Lists.newArrayListWithCapacity(matchingActiveTasks.size());

    for (SingularityTaskId taskId : matchingActiveTasks) {
      Collection<SingularityTaskHistoryUpdate> updates = taskUpdates.get(taskId);
      SimplifiedTaskState currentState = SingularityTaskHistoryUpdate.getCurrentState(updates);
      if (currentState == SimplifiedTaskState.RUNNING && isRunningTaskHealthy(deploy, updates, taskId)) {
        healthyTaskIds.add(taskId);
      }
    }

    return healthyTaskIds;
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
      DeployHealth individualTaskHealth = getTaskHealth(deploy, isDeployPending, Optional.fromNullable(healthcheckResults.get(taskId)), taskId);
      if (individualTaskHealth != DeployHealth.HEALTHY) {
        return individualTaskHealth;
      }
    }
    return DeployHealth.HEALTHY;
  }

  private List<SingularityTaskId> getHealthcheckedHealthyTasks(final SingularityDeploy deploy, final Collection<SingularityTaskId> matchingActiveTasks, final boolean isDeployPending) {
    final Map<SingularityTaskId, SingularityTaskHealthcheckResult> healthcheckResults = taskManager.getLastHealthcheck(matchingActiveTasks);
    final List<SingularityTaskId> healthyTaskIds = Lists.newArrayListWithCapacity(matchingActiveTasks.size());

    for (SingularityTaskId taskId : matchingActiveTasks) {
      DeployHealth individualTaskHealth = getTaskHealth(deploy, isDeployPending, Optional.fromNullable(healthcheckResults.get(taskId)), taskId);
      if (individualTaskHealth == DeployHealth.HEALTHY) {
        healthyTaskIds.add(taskId);
      }
    }

    return healthyTaskIds;
  }

  public DeployHealth getTaskHealth(SingularityDeploy deploy, boolean isDeployPending, Optional<SingularityTaskHealthcheckResult> healthcheckResult, SingularityTaskId taskId) {
    if (!healthcheckResult.isPresent()) {
      LOG.debug("No healthcheck present for {}", taskId);
      return DeployHealth.WAITING;
    } else if (healthcheckResult.get().isFailed()) {
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

  public List<SingularityDeployFailure> getTaskFailures(final Optional<SingularityDeploy> deploy, final Collection<SingularityTaskId> activeTasks) {
    List<SingularityDeployFailure> failures = new ArrayList<>();
    Map<SingularityTaskId, List<SingularityTaskHistoryUpdate>> taskUpdates = taskManager.getTaskHistoryUpdates(activeTasks);
    Map<SingularityTaskId, SingularityTaskHealthcheckResult> healthcheckResults = taskManager.getLastHealthcheck(activeTasks);

    for (SingularityTaskId taskId : activeTasks) {
      Optional<SingularityDeployFailure> maybeFailure = getTaskFailure(deploy.get(), taskUpdates, healthcheckResults, taskId);
      if (maybeFailure.isPresent()) {
        failures.add(maybeFailure.get());
      }
    }
    return failures;
  }

  private Optional<SingularityDeployFailure> getTaskFailure(SingularityDeploy deploy, Map<SingularityTaskId, List<SingularityTaskHistoryUpdate>> taskUpdates,
    Map<SingularityTaskId, SingularityTaskHealthcheckResult> healthcheckResults, SingularityTaskId taskId) {
    SingularityTaskHealthcheckResult healthcheckResult = healthcheckResults.get(taskId);
    Optional<SingularityDeployFailure> maybeFailure;
    if (healthcheckResult == null) {
      maybeFailure = getNonHealthcheckedTaskFailure(taskUpdates, taskId);
    } else {
      maybeFailure = getHealthcheckedTaskFailure(deploy, taskUpdates, healthcheckResult, taskId);
    }
    return maybeFailure;
  }

  private Optional<SingularityDeployFailure> getHealthcheckedTaskFailure(SingularityDeploy deploy, Map<SingularityTaskId, List<SingularityTaskHistoryUpdate>> taskUpdates,
    SingularityTaskHealthcheckResult healthcheckResult, SingularityTaskId taskId) {
    Collection<SingularityTaskHistoryUpdate> updates = taskUpdates.get(taskId);

    if (!healthcheckResult.isFailed()) {
      return Optional.absent();
    }

    SingularityTaskHistoryUpdate lastUpdate = Iterables.getLast(updates);
    if (lastUpdate.getTaskState().isDone()) {
      if (lastUpdate.getTaskState().isSuccess()) {
        return Optional.of(new SingularityDeployFailure(SingularityDeployFailureReason.TASK_EXPECTED_RUNNING_FINISHED, Optional.of(taskId),
          Optional.of(String.format("Task was expected to maintain TASK_RUNNING state but finished. (%s)", lastUpdate.getStatusMessage().or("")))));
      } else {
        return Optional.of(new SingularityDeployFailure(SingularityDeployFailureReason.TASK_FAILED_ON_STARTUP, Optional.of(taskId), lastUpdate.getStatusMessage()));
      }
    }

    final Optional<Integer> healthcheckMaxRetries = deploy.getHealthcheckMaxRetries().or(configuration.getHealthcheckMaxRetries());
    if (healthcheckMaxRetries.isPresent() && taskManager.getNumHealthchecks(taskId) > healthcheckMaxRetries.get()) {
      String message = String.format("Instance %s failed %s healthchecks, the max for the deploy.", taskId.getInstanceNo(), healthcheckMaxRetries.get() + 1);
      if (healthcheckResult.getStatusCode().isPresent()) {
        message = String.format("%s Last check returned with status code %s", message, healthcheckResult.getStatusCode().get());
      }
      return Optional.of(new SingularityDeployFailure(SingularityDeployFailureReason.TASK_FAILED_HEALTH_CHECKS, Optional.of(taskId), Optional.of(message)));
    }

    long runningAt = getRunningAt(updates);
    final long durationSinceRunning = System.currentTimeMillis() - runningAt;
    if (isRunningLongerThanThreshold(deploy, durationSinceRunning)) {
      String message = String.format("Instance %s has been running for %s and has yet to pass healthchecks.", taskId.getInstanceNo(), JavaUtils.durationFromMillis(durationSinceRunning));
      if (healthcheckResult.getStatusCode().isPresent()) {
        message = String.format("%s Last check returned with status code %s", message, healthcheckResult.getStatusCode().get());
      }
      return Optional.of(new SingularityDeployFailure(SingularityDeployFailureReason.TASK_FAILED_HEALTH_CHECKS, Optional.of(taskId), Optional.of(message)));
    }

    return Optional.absent();
  }

  private boolean isRunningLongerThanThreshold(SingularityDeploy deploy, long durationSinceRunning) {
    long relevantTimeoutSeconds = deploy.getHealthcheckMaxTotalTimeoutSeconds().or(configuration.getHealthcheckMaxTotalTimeoutSeconds()).or(deploy.getDeployHealthTimeoutSeconds()).or(
      configuration.getDeployHealthyBySeconds());
    return durationSinceRunning > TimeUnit.SECONDS.toMillis(relevantTimeoutSeconds);
  }

  private long getRunningAt(Collection<SingularityTaskHistoryUpdate> updates) {
    long runningAt = 0;

    for (SingularityTaskHistoryUpdate update : updates) {
      if (update.getTaskState() == ExtendedTaskState.TASK_RUNNING) {
        runningAt = update.getTimestamp();
        break;
      }
    }

    return runningAt;
  }

  private Optional<SingularityDeployFailure> getNonHealthcheckedTaskFailure(Map<SingularityTaskId, List<SingularityTaskHistoryUpdate>> taskUpdates, SingularityTaskId taskId) {
    List<SingularityTaskHistoryUpdate> updates = taskUpdates.get(taskId);
    SingularityTaskHistoryUpdate lastUpdate = Iterables.getLast(updates);

    if (lastUpdate.getTaskState().isSuccess()) {
      return Optional.of(new SingularityDeployFailure(SingularityDeployFailureReason.TASK_EXPECTED_RUNNING_FINISHED, Optional.of(taskId),
        Optional.of(String.format("Task was expected to maintain TASK_RUNNING state but finished. (%s)", lastUpdate.getStatusMessage().or("")))));
    } else if (lastUpdate.getTaskState().isDone()) {
      return Optional.of(new SingularityDeployFailure(SingularityDeployFailureReason.TASK_FAILED_ON_STARTUP, Optional.of(taskId), lastUpdate.getStatusMessage()));
    } else if (SingularityTaskHistoryUpdate.getCurrentState(updates) == SimplifiedTaskState.WAITING) {
      return Optional.of(new SingularityDeployFailure(SingularityDeployFailureReason.TASK_NEVER_ENTERED_RUNNING, Optional.of(taskId),
        Optional.of(String.format("Task never entered running state, last state was %s (%s)", lastUpdate.getTaskState().getDisplayName(), lastUpdate.getStatusMessage().or("")))));
    }
    return Optional.absent();
  }
}
