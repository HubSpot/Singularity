package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;

import org.apache.mesos.Protos.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHealthcheckResult;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;

public class SingularityHealthChecker {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityHealthChecker.class);

  private final AsyncHttpClient http;
  private final SingularityConfiguration configuration;
  
  @Inject
  public SingularityHealthChecker(AsyncHttpClient http, SingularityConfiguration configuration) {
    this.http = http;
    this.configuration = configuration;
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
  
  private SingularityTaskHealthcheckResult failedResult(long now, Optional<Long> duration, String failureMessage, SingularityTask task) {
    return new SingularityTaskHealthcheckResult(Optional.<Integer> absent(), duration, now, Optional.<String> absent(), Optional.of(failureMessage), task.getTaskId().getId());
  }
  
  public SingularityTaskHealthcheckResult healthcheck(SingularityTask task) {
    final long now = System.currentTimeMillis();
    final Optional<String> uri = getHealthcheckUri(task);
    
    if (!uri.isPresent()) {
      return failedResult(now, Optional.<Long> absent(), "Healthcheck uri or ports not present", task);
    }
    
    final Long timeoutSeconds = task.getTaskRequest().getDeploy().getHealthcheckTimeoutSeconds().or(configuration.getDefaultHealthcheckTimeoutSeconds());
    
    ListenableFuture<Response> responseFuture = null;
    
    try {
      responseFuture = http.prepareGet(uri.get()).execute();
      
      Response response = responseFuture.get(timeoutSeconds, TimeUnit.SECONDS);
      
      Optional<String> responseBody = Optional.absent();
      
      if (response.hasResponseBody()) {
        responseBody = Optional.of(response.getResponseBody());
      }
      
      return new SingularityTaskHealthcheckResult(Optional.of(response.getStatusCode()), Optional.of(System.currentTimeMillis() - now), now, responseBody, Optional.<String> absent(), task.getTaskId().getId());
    } catch (Exception e) {
      if (responseFuture != null) {
        responseFuture.cancel(true);
      }
      
      return failedResult(now, Optional.of(System.currentTimeMillis() - now), String.format("Healthcheck failed due to exception: %s", e.getMessage()), task);
    }
  }
  
  
}
