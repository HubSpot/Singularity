package com.hubspot.singularity.scheduler;

import java.net.ConnectException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHealthcheckResult;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

public class SingularityHealthcheckAsyncHandler {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityHealthchecker.class);

  private final long startTime;
  private final SingularityExceptionNotifier exceptionNotifier;
  private final SingularityHealthchecker healthchecker;
  private final SingularityNewTaskChecker newTaskChecker;
  private final SingularityTask task;
  private final TaskManager taskManager;
  private final List<Integer> failureStatusCodes;
  private String healthcheckUri = ""; // For logging purposes only

  public SingularityHealthcheckAsyncHandler(SingularityExceptionNotifier exceptionNotifier, SingularityConfiguration configuration, SingularityHealthchecker healthchecker,
      SingularityNewTaskChecker newTaskChecker, TaskManager taskManager, SingularityTask task) {
    this.exceptionNotifier = exceptionNotifier;
    this.taskManager = taskManager;
    this.newTaskChecker = newTaskChecker;
    this.healthchecker = healthchecker;
    this.task = task;
    this.failureStatusCodes = task.getTaskRequest().getDeploy().getHealthcheck().isPresent() ?
      task.getTaskRequest().getDeploy().getHealthcheck().get().getFailureStatusCodes().or(configuration.getHealthcheckFailureStatusCodes()) :
      configuration.getHealthcheckFailureStatusCodes();

    startTime = System.currentTimeMillis();
  }

  public void setHealthcheckUri(String healthcheckUri) {
    this.healthcheckUri = healthcheckUri;
  }

  public void onCompleted(Optional<Integer> statusCode, Optional<String> responseBodyExcerpt) {
    saveResult(statusCode, responseBodyExcerpt, Optional.<String> absent(), Optional.<Throwable>absent());
  }

  public void onFailed(Throwable t) {
    LOG.trace("Exception while making health check for task {}", task.getTaskId(), t);

    saveResult(Optional.<Integer> absent(), Optional.<String> absent(), Optional.of(String.format("Healthcheck (%s) failed due to exception: %s", healthcheckUri, t.getMessage())), Optional.of(t));
  }

  public void saveResult(Optional<Integer> statusCode, Optional<String> responseBody, Optional<String> errorMessage, Optional<Throwable> throwable) {
    boolean inStartup = throwable.isPresent() && throwable.get() instanceof ConnectException;

    try {
      SingularityTaskHealthcheckResult result = new SingularityTaskHealthcheckResult(statusCode, Optional.of(System.currentTimeMillis() - startTime), startTime, responseBody,
          errorMessage, task.getTaskId(), Optional.of(inStartup));

      LOG.trace("Saving healthcheck result {}", result);

      taskManager.saveHealthcheckResult(result);

      if (result.isFailed()) {
        if (!taskManager.isActiveTask(task.getTaskId())) {
          LOG.trace("Task {} is not active, not re-enqueueing healthcheck", task.getTaskId());
          return;
        }

        if (statusCode.isPresent() && failureStatusCodes.contains(statusCode.get())) {
          LOG.debug("Failed status code present for task {} ({})", task.getTaskId(), statusCode.get());
          healthchecker.markHealthcheckFinished(task.getTaskId().getId());
          newTaskChecker.runNewTaskCheckImmediately(task, healthchecker);
          return;
        }

        healthchecker.enqueueHealthcheck(task, true, inStartup, false);
      } else {
        healthchecker.markHealthcheckFinished(task.getTaskId().getId());

        newTaskChecker.runNewTaskCheckImmediately(task, healthchecker);
      }
    } catch (Throwable t) {
      LOG.error("Caught throwable while saving health check result for {}, will re-enqueue", task.getTaskId(), t);
      exceptionNotifier.notify(String.format("Error saving healthcheck (%s)", t.getMessage()), t, ImmutableMap.of("taskId", task.getTaskId().toString()));

      healthchecker.reEnqueueOrAbort(task, inStartup);
    }
  }


}
