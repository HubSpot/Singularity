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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.deploy.HealthcheckOptions;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployFailure;
import com.hubspot.singularity.SingularityDeployFailureReason;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHealthcheckResult;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskHistoryUpdate.SimplifiedTaskState;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;

@Singleton
public class SingularityDeployHealthHelper {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityDeployHealthHelper.class);

  private final TaskManager taskManager;
  private final SingularityConfiguration configuration;
  private final RequestManager requestManager;

  @Inject
  public SingularityDeployHealthHelper(TaskManager taskManager, SingularityConfiguration configuration, RequestManager requestManager) {
    this.taskManager = taskManager;
    this.configuration = configuration;
    this.requestManager = requestManager;
  }

  public enum DeployHealth {
    WAITING, UNHEALTHY, HEALTHY;
  }

  private boolean shouldCheckHealthchecks(final SingularityRequest request, final Optional<SingularityDeploy> deploy, final Collection<SingularityTaskId> activeTasks, final boolean isDeployPending) {
    if (!deploy.isPresent()) {
      return false;
    }

    if (!deploy.get().getHealthcheck().isPresent()) {
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
    List<SingularityRequestHistory> requestHistories = requestManager.getRequestHistory(deploy.getRequestId());

    for (SingularityTaskId taskId : matchingActiveTasks) {
      DeployHealth individualTaskHealth;
      if (healthchecksSkipped(taskId, requestHistories, deploy)) {
        LOG.trace("Detected skipped healthchecks for {}", taskId);
        individualTaskHealth = DeployHealth.HEALTHY;
      } else {
        individualTaskHealth = getTaskHealth(deploy, isDeployPending, Optional.fromNullable(healthcheckResults.get(taskId)), taskId);
      }
      if (individualTaskHealth != DeployHealth.HEALTHY) {
        return individualTaskHealth;
      }
    }
    return DeployHealth.HEALTHY;
  }

  private List<SingularityTaskId> getHealthcheckedHealthyTasks(final SingularityDeploy deploy, final Collection<SingularityTaskId> matchingActiveTasks, final boolean isDeployPending) {
    final Map<SingularityTaskId, SingularityTaskHealthcheckResult> healthcheckResults = taskManager.getLastHealthcheck(matchingActiveTasks);
    final List<SingularityTaskId> healthyTaskIds = Lists.newArrayListWithCapacity(matchingActiveTasks.size());
    List<SingularityRequestHistory> requestHistories = requestManager.getRequestHistory(deploy.getRequestId());

    for (SingularityTaskId taskId : matchingActiveTasks) {
      DeployHealth individualTaskHealth;
      if (healthchecksSkipped(taskId, requestHistories, deploy)) {
        LOG.trace("Detected skipped healthchecks for {}", taskId);
        individualTaskHealth = DeployHealth.HEALTHY;
      } else {
        individualTaskHealth = getTaskHealth(deploy, isDeployPending, Optional.fromNullable(healthcheckResults.get(taskId)), taskId);
      }
      if (individualTaskHealth == DeployHealth.HEALTHY) {
        healthyTaskIds.add(taskId);
      }
    }

    return healthyTaskIds;
  }

  private boolean healthchecksSkipped(SingularityTaskId taskId, List<SingularityRequestHistory> requestHistories, SingularityDeploy deploy) {
    if (deploy.getSkipHealthchecksOnDeploy().or(false)) {
      return true;
    }

    Optional<SingularityTask> maybeTask = taskManager.getTask(taskId);
    if (maybeTask.isPresent()) {
      if (maybeTask.get().getTaskRequest().getPendingTask().getSkipHealthchecks().or(false)) {
        return true;
      }

      Optional<Long> runningStartTime = getRunningAt(taskManager.getTaskHistoryUpdates(taskId));
      if (runningStartTime.isPresent()) {
        Optional<SingularityRequestHistory> previousHistory = Optional.absent();
        for (SingularityRequestHistory history : requestHistories) {
          if (history.getCreatedAt() < runningStartTime.get() && (!previousHistory.isPresent() || previousHistory.get().getCreatedAt() < history.getCreatedAt())) {
            previousHistory = Optional.of(history);
          }
        }

        if (previousHistory.isPresent() && previousHistory.get().getRequest().getSkipHealthchecks().or(false)) {
          return true;
        }
      }
    }


    return false;
  }

  public DeployHealth getTaskHealth(SingularityDeploy deploy, boolean isDeployPending, Optional<SingularityTaskHealthcheckResult> healthcheckResult, SingularityTaskId taskId) {
    if (!healthcheckResult.isPresent()) {
      LOG.debug("No healthcheck present for {}", taskId);
      return DeployHealth.WAITING;
    } else if (healthcheckResult.get().isFailed()) {
      LOG.debug("Found a failed healthcheck: {}", healthcheckResult);

      if (deploy.getHealthcheck().isPresent() && healthcheckResult.get().getStatusCode().isPresent()
        && deploy.getHealthcheck().get().getFailureStatusCodes().or(configuration.getHealthcheckFailureStatusCodes()).contains(healthcheckResult.get().getStatusCode().get())) {
        LOG.debug("Failed healthcheck had bad status code: {}", healthcheckResult.get().getStatusCode().get());
        return DeployHealth.UNHEALTHY;
      }

      final int startupTimeout = deploy.getHealthcheck().isPresent() ? deploy.getHealthcheck().get().getStartupTimeoutSeconds().or(configuration.getStartupTimeoutSeconds()) : configuration.getStartupTimeoutSeconds();
      Collection<SingularityTaskHistoryUpdate> updates = taskManager.getTaskHistoryUpdates(taskId);
      Optional<Long> runningAt = getRunningAt(updates);
      if (runningAt.isPresent()) {
        final long durationSinceRunning = System.currentTimeMillis() - runningAt.get();
        if (healthcheckResult.get().isStartup() && durationSinceRunning > TimeUnit.SECONDS.toMillis(startupTimeout)) {
          LOG.debug("{} has not responded to healthchecks in {}s", taskId, startupTimeout);
          return DeployHealth.UNHEALTHY;
        }
      }

      final Optional<Integer> healthcheckMaxRetries = deploy.getHealthcheck().isPresent() ? deploy.getHealthcheck().get().getMaxRetries().or(configuration.getHealthcheckMaxRetries()) : Optional.<Integer>absent();

      if (healthcheckMaxRetries.isPresent() && taskManager.getNumNonstartupHealthchecks(taskId) > healthcheckMaxRetries.get()) {
        LOG.debug("{} failed {} healthchecks, the max for the deploy", taskId, healthcheckMaxRetries.get());
        return DeployHealth.UNHEALTHY;
      }

      final Optional<Integer> healthcheckMaxTotalTimeoutSeconds = deploy.getHealthcheck().isPresent() ? Optional.of(getMaxHealthcheckTimeoutSeconds(deploy.getHealthcheck().get())) : Optional.<Integer>absent();

      if (isDeployPending && healthcheckMaxTotalTimeoutSeconds.isPresent()) {
        if (runningAt.isPresent()) {
          final long durationSinceRunning = System.currentTimeMillis() - runningAt.get();
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

  public int getMaxHealthcheckTimeoutSeconds(HealthcheckOptions options) {
    int intervalSeconds = options.getIntervalSeconds().or(configuration.getHealthcheckIntervalSeconds());
    int responseTimeSeconds = options.getResponseTimeoutSeconds().or(configuration.getHealthcheckTimeoutSeconds());
    int startupTime = options.getStartupTimeoutSeconds().or(configuration.getStartupTimeoutSeconds());
    int attempts = options.getMaxRetries().or(configuration.getHealthcheckMaxRetries()).or(0) + 1;
    return startupTime + ((intervalSeconds + responseTimeSeconds) * attempts);
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

    final Optional<Integer> healthcheckMaxRetries = deploy.getHealthcheck().isPresent() ?
      deploy.getHealthcheck().get().getMaxRetries().or(configuration.getHealthcheckMaxRetries()) : configuration.getHealthcheckMaxRetries();
    if (healthcheckMaxRetries.isPresent() && taskManager.getNumNonstartupHealthchecks(taskId) > healthcheckMaxRetries.get()) {
      String message = String.format("Instance %s failed %s healthchecks, the max for the deploy.", taskId.getInstanceNo(), healthcheckMaxRetries.get() + 1);
      if (healthcheckResult.getStatusCode().isPresent()) {
        message = String.format("%s Last check returned with status code %s", message, healthcheckResult.getStatusCode().get());
      }
      return Optional.of(new SingularityDeployFailure(SingularityDeployFailureReason.TASK_FAILED_HEALTH_CHECKS, Optional.of(taskId), Optional.of(message)));
    }

    Optional<Long> runningAt = getRunningAt(updates);
    if (runningAt.isPresent()) {
      final long durationSinceRunning = System.currentTimeMillis() - runningAt.get();
      if (healthcheckResult.isStartup() && deploy.getHealthcheck().isPresent() && durationSinceRunning > deploy.getHealthcheck().get().getStartupTimeoutSeconds()
        .or(configuration.getStartupTimeoutSeconds())) {
        String message = String.format("Instance %s has not responded to healthchecks after running for %s", taskId.getInstanceNo(), JavaUtils.durationFromMillis(durationSinceRunning));
        return Optional.of(new SingularityDeployFailure(SingularityDeployFailureReason.TASK_FAILED_HEALTH_CHECKS, Optional.of(taskId), Optional.of(message)));
      }
      if (isRunningLongerThanThreshold(deploy, durationSinceRunning)) {
        String message = String.format("Instance %s has been running for %s and has yet to pass healthchecks.", taskId.getInstanceNo(), JavaUtils.durationFromMillis(durationSinceRunning));
        if (healthcheckResult.getStatusCode().isPresent()) {
          message = String.format("%s Last check returned with status code %s", message, healthcheckResult.getStatusCode().get());
        }
        return Optional.of(new SingularityDeployFailure(SingularityDeployFailureReason.TASK_FAILED_HEALTH_CHECKS, Optional.of(taskId), Optional.of(message)));
      }
    }

    return Optional.absent();
  }

  private boolean isRunningLongerThanThreshold(SingularityDeploy deploy, long durationSinceRunning) {
    long relevantTimeoutSeconds = deploy.getHealthcheck().isPresent() ?
      getMaxHealthcheckTimeoutSeconds(deploy.getHealthcheck().get()) : deploy.getDeployHealthTimeoutSeconds().or(configuration.getDeployHealthyBySeconds());
    return durationSinceRunning > TimeUnit.SECONDS.toMillis(relevantTimeoutSeconds);
  }

  private Optional<Long> getRunningAt(Collection<SingularityTaskHistoryUpdate> updates) {
    for (SingularityTaskHistoryUpdate update : updates) {
      if (update.getTaskState() == ExtendedTaskState.TASK_RUNNING) {
        return  Optional.of(update.getTimestamp());
      }
    }

    return Optional.absent();
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
