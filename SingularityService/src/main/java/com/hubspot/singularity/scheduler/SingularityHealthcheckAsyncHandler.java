package com.hubspot.singularity.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHealthcheckResult;
import com.hubspot.singularity.data.TaskManager;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;

public class SingularityHealthcheckAsyncHandler extends AsyncCompletionHandler<Response> {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityHealthchecker.class);

  private final long startTime;
  private final SingularityHealthchecker healthchecker;
  private final SingularityTask task;
  private final TaskManager taskManager;
  private final SingularityAbort abort;
  
  public SingularityHealthcheckAsyncHandler(SingularityHealthchecker healthchecker, TaskManager taskManager, SingularityAbort abort, SingularityTask task) {
    this.taskManager = taskManager;
    this.healthchecker = healthchecker;
    this.abort = abort;
    this.task = task;
    
    startTime = System.currentTimeMillis();
  }
  
  @Override
  public Response onCompleted(Response response) throws Exception {
    Optional<String> responseBody = Optional.absent();
    
    if (response.hasResponseBody()) {
      responseBody = Optional.of(response.getResponseBody());
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
    SingularityTaskHealthcheckResult result = new SingularityTaskHealthcheckResult(statusCode, Optional.of(System.currentTimeMillis() - startTime), startTime, responseBody, 
        errorMessage, task.getTaskId());
  
    LOG.trace("Saving healthcheck result {}", result);
 
    try {
      taskManager.saveHealthcheckResult(result);
      
      if (result.isFailed()) {
        healthchecker.reEnqueueHealthcheck(task);
      }
    } catch (Throwable t) {
      LOG.error("Aborting, caught throwable while saving health check result {}", result, t);
      
      abort.abort();
    }
  }

}
