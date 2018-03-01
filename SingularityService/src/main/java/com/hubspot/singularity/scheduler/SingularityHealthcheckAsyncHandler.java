package com.hubspot.singularity.scheduler;

import java.net.ConnectException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.hubspot.singularity.api.task.SingularityTask;
import com.hubspot.singularity.api.task.SingularityTaskHealthcheckResult;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;

public class SingularityHealthcheckAsyncHandler extends AsyncCompletionHandler<Response> {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityHealthchecker.class);

  private final long startTime;
  private final SingularityExceptionNotifier exceptionNotifier;
  private final SingularityHealthchecker healthchecker;
  private final SingularityNewTaskChecker newTaskChecker;
  private final SingularityTask task;
  private final TaskManager taskManager;
  private final int maxHealthcheckResponseBodyBytes;
  private final List<Integer> failureStatusCodes;

  public SingularityHealthcheckAsyncHandler(SingularityExceptionNotifier exceptionNotifier, SingularityConfiguration configuration, SingularityHealthchecker healthchecker,
      SingularityNewTaskChecker newTaskChecker, TaskManager taskManager, SingularityTask task) {
    this.exceptionNotifier = exceptionNotifier;
    this.taskManager = taskManager;
    this.newTaskChecker = newTaskChecker;
    this.healthchecker = healthchecker;
    this.task = task;
    this.maxHealthcheckResponseBodyBytes = configuration.getMaxHealthcheckResponseBodyBytes();
    this.failureStatusCodes = task.getTaskRequest().getDeploy().getHealthcheck().isPresent() ?
      task.getTaskRequest().getDeploy().getHealthcheck().get().getFailureStatusCodes().orElse(configuration.getHealthcheckFailureStatusCodes()) :
      configuration.getHealthcheckFailureStatusCodes();

    startTime = System.currentTimeMillis();
  }

  @Override
  public Response onCompleted(Response response) throws Exception {
    Optional<String> responseBody = Optional.empty();

    if (response.hasResponseBody()) {
      responseBody = Optional.of(response.getResponseBodyExcerpt(maxHealthcheckResponseBodyBytes));
    }

    saveResult(Optional.of(response.getStatusCode()), responseBody, Optional.empty(), Optional.empty());

    return response;
  }

  @Override
  public void onThrowable(Throwable t) {
    LOG.trace("Exception while making health check for task {}", task.getTaskId(), t);

    saveResult(Optional.empty(), Optional.empty(), Optional.of(String.format("Healthcheck failed due to exception: %s", t.getMessage())), Optional.of(t));
  }

  public void saveResult(Optional<Integer> statusCode, Optional<String> responseBody, Optional<String> errorMessage, Optional<Throwable> throwable) {
    boolean inStartup = throwable.isPresent() && throwable.get() instanceof ConnectException;

    try {
      SingularityTaskHealthcheckResult result = SingularityTaskHealthcheckResult.builder()
          .setStatusCode(statusCode)
          .setDurationMillis(System.currentTimeMillis() - startTime)
          .setTimestamp(startTime)
          .setResponseBody(responseBody)
          .setErrorMessage(errorMessage)
          .setTaskId(task.getTaskId())
          .setStartup(inStartup)
          .build();

      LOG.trace("Saving healthcheck result {}", result);

      taskManager.saveHealthcheckResult(result);

      if (result.isFailed()) {
        if (!taskManager.isActiveTask(task.getTaskId().getId())) {
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
