package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;

import org.apache.mesos.Protos.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.TaskManager;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.PerRequestConfig;
import com.ning.http.client.RequestBuilder;

@SuppressWarnings("deprecation")
public class SingularityHealthchecker {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityHealthchecker.class);

  private final AsyncHttpClient http;
  private final SingularityConfiguration configuration;
  private final TaskManager taskManager;
  private final SingularityAbort abort;
  
  @Inject
  public SingularityHealthchecker(AsyncHttpClient http, SingularityConfiguration configuration, TaskManager taskManager, SingularityAbort abort) {
    this.http = http;
    this.configuration = configuration;
    this.taskManager = taskManager;
    this.abort = abort;
  }
  
  private Optional<String> getHealthcheckUri(SingularityTask task) {
    if (task.getTaskRequest().getDeploy().getHealthcheckUri() == null) {
      return Optional.absent();
    }
    
    final String hostname = task.getOffer().getHostname();
    
    long firstPort = 0;
    
    for (Resource resource : task.getMesosTask().getResourcesList()) {
      if (resource.getName().equals(MesosUtils.PORTS)) {
        if (resource.getRanges().getRangeCount() > 0) {
          firstPort = resource.getRanges().getRange(0).getBegin();
        }
      }
    }
    
    if (firstPort < 0) {
      LOG.warn(String.format("Couldn't find a port for health check for task %s", task));
      return Optional.absent();
    }
    
    return Optional.of(String.format("http://%s:%s/%s", hostname, firstPort, task.getTaskRequest().getDeploy().getHealthcheckUri()));
  }
  
  private void saveFailure(SingularityHealthcheckAsyncHandler handler, String message) {
    handler.saveResult(Optional.<Integer> absent(), Optional.<String> absent(), Optional.of(message));
  }
  
  public void healthcheck(final SingularityTask task) {
    final SingularityHealthcheckAsyncHandler handler = new SingularityHealthcheckAsyncHandler(taskManager, abort, task);
    final Optional<String> uri = getHealthcheckUri(task);
    
    if (!uri.isPresent()) {
      saveFailure(handler, "Healthcheck uri or ports not present");
      return;
    }
    
    final Long timeoutSeconds = task.getTaskRequest().getDeploy().getHealthcheckTimeoutSeconds().or(configuration.getDefaultHealthcheckTimeoutSeconds());
    
    try {
      PerRequestConfig prc = new PerRequestConfig();
      prc.setRequestTimeoutInMs((int) TimeUnit.SECONDS.toMillis(timeoutSeconds));
      
      RequestBuilder builder = new RequestBuilder("GET");
      builder.setUrl(uri.get());
      builder.setPerRequestConfig(prc);
      
      http.prepareRequest(builder.build()).execute(handler);
    } catch (Throwable t) {
      LOG.debug(String.format("Exception while preparing healthcheck (%s) for task (%s)", uri, task.getTaskId()), t);
      
      saveFailure(handler, String.format("Healthcheck failed due to exception: %s", t.getMessage()));
    }
  }
  
}
