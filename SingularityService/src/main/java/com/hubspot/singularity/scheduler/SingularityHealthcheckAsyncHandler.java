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
  private final SingularityTask task;
  private final TaskManager taskManager;
  private final SingularityAbort abort;
  
  public SingularityHealthcheckAsyncHandler(TaskManager taskManager, SingularityAbort abort, SingularityTask task) {
    this.taskManager = taskManager;
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
    LOG.trace(String.format("Exception while making health check for task %s", task.getTaskId()), t);
  
    saveResult(Optional.<Integer> absent(), Optional.<String> absent(), Optional.of(String.format("Healthcheck failed due to exception: %s", t.getMessage())));
  }
  
  public void saveResult(Optional<Integer> statusCode, Optional<String> responseBody, Optional<String> errorMessage) {
    SingularityTaskHealthcheckResult result = new SingularityTaskHealthcheckResult(statusCode, Optional.of(System.currentTimeMillis() - startTime), startTime, responseBody, 
        errorMessage, task.getTaskId());
  
    LOG.trace(String.format("Saving healthcheck result %s", result));
 
    try {
      taskManager.saveHealthcheckResult(result);
    } catch (Throwable t) {
      LOG.error(String.format("Aborting, caught throwable while saving health check result %s", result), t);
      
      abort.abort();
    }
  }

}
