package com.hubspot.singularity.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHealthcheckResult;
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

  public SingularityHealthcheckAsyncHandler(SingularityExceptionNotifier exceptionNotifier, SingularityConfiguration configuration, SingularityHealthchecker healthchecker,
      SingularityNewTaskChecker newTaskChecker, TaskManager taskManager, SingularityTask task) {
    this.exceptionNotifier = exceptionNotifier;
    this.taskManager = taskManager;
    this.newTaskChecker = newTaskChecker;
    this.healthchecker = healthchecker;
    this.task = task;
    this.maxHealthcheckResponseBodyBytes = configuration.getMaxHealthcheckResponseBodyBytes();

    startTime = System.currentTimeMillis();
  }

  @Override
  public Response onCompleted(Response response) throws Exception {
    Optional<String> responseBody = Optional.absent();

    if (response.hasResponseBody()) {
      responseBody = Optional.of(response.getResponseBodyExcerpt(maxHealthcheckResponseBodyBytes));
    }

    saveResult(Optional.of(response.getStatusCode()), responseBody, Optional.<String> absent());

    return response;
  }

  @Override
  public void onThrowable(Throwable t) {
    LOG.trace("Exception while making health check for task {}", task.getTaskId(), t);

    saveResult(Optional.<Integer> absent(), Optional.<String> absent(), Optional.of(String.format("Healthcheck failed due to exception: %s", t.getMessage())));
  }

  public void saveResult(Optional<Integer> statusCode, Optional<String> responseBody, Optional<String> errorMessage) {
    try {
      SingularityTaskHealthcheckResult result = new SingularityTaskHealthcheckResult(statusCode, Optional.of(System.currentTimeMillis() - startTime), startTime, responseBody,
          errorMessage, task.getTaskId());

      LOG.trace("Saving healthcheck result {}", result);

      taskManager.saveHealthcheckResult(result);

      if (result.isFailed()) {
        if (!taskManager.isActiveTask(task.getTaskId().getId())) {
          LOG.trace("Task {} is not active, not re-enqueueing healthcheck", task.getTaskId());
          return;
        }

        healthchecker.enqueueHealthcheck(task, true);
      } else {
        healthchecker.markHealthcheckFinished(task.getTaskId().getId());

        newTaskChecker.runNewTaskCheckImmediately(task, healthchecker);
      }
    } catch (Throwable t) {
      LOG.error("Caught throwable while saving health check result for {}, will re-enqueue", task.getTaskId(), t);
      exceptionNotifier.notify(t, ImmutableMap.of("taskId", task.getTaskId().toString()));

      healthchecker.reEnqueueOrAbort(task);
    }
  }


}
