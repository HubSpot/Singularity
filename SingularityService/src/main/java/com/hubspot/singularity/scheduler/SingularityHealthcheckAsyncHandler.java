package com.hubspot.singularity.scheduler;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hubspot.singularity.SingularityAbort;
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
  private final SingularityAbort abort;
  private final int maxHealthcheckResponseBodyBytes;

  public SingularityHealthcheckAsyncHandler(SingularityExceptionNotifier exceptionNotifier, SingularityConfiguration configuration, SingularityHealthchecker healthchecker, SingularityNewTaskChecker newTaskChecker, TaskManager taskManager, SingularityAbort abort, SingularityTask task) {
    this.exceptionNotifier = exceptionNotifier;
    this.taskManager = taskManager;
    this.newTaskChecker = newTaskChecker;
    this.healthchecker = healthchecker;
    this.abort = abort;
    this.task = task;
    this.maxHealthcheckResponseBodyBytes = configuration.getMaxHealthcheckResponseBodyBytes();

    startTime = System.currentTimeMillis();
  }

  @Override
  public Response onCompleted(Response response) throws Exception {
    Optional<String> responseBody = Optional.empty();

    if (response.hasResponseBody()) {
      responseBody = Optional.of(response.getResponseBodyExcerpt(maxHealthcheckResponseBodyBytes));
    }

    saveResult(Optional.of(response.getStatusCode()), responseBody, Optional.<String> empty());

    return response;
  }

  @Override
  public void onThrowable(Throwable t) {
    LOG.trace("Exception while making health check for task {}", task.getTaskId(), t);

    saveResult(Optional.empty(), Optional.empty(), Optional.of(String.format("Healthcheck failed due to exception: %s", t.getMessage())));
  }

  public void saveResult(Optional<Integer> statusCode, Optional<String> responseBody, Optional<String> errorMessage) {
    SingularityTaskHealthcheckResult result = new SingularityTaskHealthcheckResult(statusCode, Optional.of(System.currentTimeMillis() - startTime), startTime, responseBody,
        errorMessage, task.getTaskId());

    LOG.trace("Saving healthcheck result {}", result);

    try {
      taskManager.saveHealthcheckResult(result);

      if (result.isFailed()) {
        healthchecker.reEnqueueHealthcheck(task);
      } else {
        newTaskChecker.runNewTaskCheckImmediately(task);
      }
    } catch (Throwable t) {
      LOG.error("Aborting, caught throwable while saving health check result {}", result, t);
      exceptionNotifier.notify(t);
      abort.abort();
    }
  }

}
