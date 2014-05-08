package com.hubspot.singularity.scheduler;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.mesos.Protos.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityCloseable;
import com.hubspot.singularity.SingularityCloser;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.TaskManager;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.PerRequestConfig;
import com.ning.http.client.RequestBuilder;

@SuppressWarnings("deprecation")
public class SingularityHealthchecker implements SingularityCloseable {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityHealthchecker.class);

  private final AsyncHttpClient http;
  private final SingularityConfiguration configuration;
  private final TaskManager taskManager;
  private final SingularityAbort abort;
  private final DeployManager deployManager;
  
  private final Map<String, ScheduledFuture<?>> taskIdToHealthcheck;
  
  private final ScheduledExecutorService executorService;
  private final SingularityCloser closer;
  
  @Inject
  public SingularityHealthchecker(AsyncHttpClient http, DeployManager deployManager, SingularityConfiguration configuration, TaskManager taskManager, SingularityAbort abort, SingularityCloser closer) {
    this.http = http;
    this.deployManager = deployManager;
    this.configuration = configuration;
    this.taskManager = taskManager;
    this.abort = abort;
    this.closer = closer;
    
    this.taskIdToHealthcheck = Maps.newConcurrentMap();
    
    this.executorService = Executors.newScheduledThreadPool(configuration.getHealthcheckStartThreads(), new ThreadFactoryBuilder().setNameFormat("SingularityHealthchecker-%d").build());
  }
  
  @Override
  public void close() {
    closer.shutdown(getClass().getName(), executorService, 1);
  }

  public void reEnqueueHealthcheck(SingularityTask task) {
    if (!isDeployPending(task)) {
      return;
    }

    privateEnqueueHealthcheck(task);
  }
  
  private boolean isDeployPending(SingularityTask task) {
    Optional<SingularityRequestDeployState> requestDeployState = deployManager.getRequestDeployState(task.getTaskId().getRequestId());
   
    if (!requestDeployState.isPresent() || !requestDeployState.get().getPendingDeploy().isPresent()) {
      return false;
    }
    
    return requestDeployState.get().getPendingDeploy().get().getDeployId().equals(task.getTaskId().getDeployId());
  }
  
  private void privateEnqueueHealthcheck(SingularityTask task) {
    ScheduledFuture<?> future = enqueueHealthcheckWithDelay(task, task.getTaskRequest().getDeploy().getHealthcheckIntervalSeconds().or(configuration.getHealthcheckIntervalSeconds()));
    
    ScheduledFuture<?> existing = taskIdToHealthcheck.put(task.getTaskId().getId(), future);
  
    if (existing != null) {
      boolean canceledExisting = existing.cancel(false);
      LOG.warn("Found existing overlapping healthcheck for task {} - cancel success: {}", task.getTaskId(), canceledExisting);
    }  
  }
  
  public void enqueueHealthcheck(SingularityTask task) {
    if (!shouldHealthcheck(task)) {
      return;
    }
    
    privateEnqueueHealthcheck(task);
  }

  public void cancelHealthcheck(String taskId) {
    ScheduledFuture<?> future = taskIdToHealthcheck.get(taskId);
    
    if (future == null) {
      return;
    }
    
    boolean canceled = future.cancel(false);
    
    LOG.trace("Canceling healthcheck ({}) for task {}", canceled, taskId);
  }
  
  private ScheduledFuture<?> enqueueHealthcheckWithDelay(final SingularityTask task, long delaySeconds) {
    LOG.trace("Enqueing a healthcheck for task {} with delay {}", task.getTaskId(), DurationFormatUtils.formatDurationHMS(TimeUnit.SECONDS.toMillis(delaySeconds)));
    
    return executorService.schedule(new Runnable() {
      
      @Override
      public void run() {
        taskIdToHealthcheck.remove(task.getTaskId().getId());
        
        try {
          asyncHealthcheck(task);
        } catch (Throwable t) {
          LOG.error("Uncaught throwable in async healthcheck", t);
        }
      }
      
    }, delaySeconds, TimeUnit.SECONDS);
  }
  
  private Optional<String> getHealthcheckUri(SingularityTask task) {
    if (task.getTaskRequest().getDeploy().getHealthcheckUri() == null) {
      return Optional.absent();
    }
    
    final String hostname = task.getOffer().getHostname();
    
    Optional<Long> firstPort = Optional.absent();
    
    for (Resource resource : task.getMesosTask().getResourcesList()) {
      if (resource.getName().equals(MesosUtils.PORTS)) {
        if (resource.getRanges().getRangeCount() > 0) {
          firstPort = Optional.of(resource.getRanges().getRange(0).getBegin());
          break;
        }
      }
    }
    
    if (!firstPort.isPresent() || firstPort.get() < 1L) {
      LOG.warn("Couldn't find a port for health check for task {}", task);
      return Optional.absent();
    }
    
    String uri = task.getTaskRequest().getDeploy().getHealthcheckUri().get();
    
    if (uri.startsWith("/")) {
      uri = uri.substring(1);
    }
    
    return Optional.of(String.format("http://%s:%d/%s", hostname, firstPort.get(), uri));
  }
  
  private void saveFailure(SingularityHealthcheckAsyncHandler handler, String message) {
    handler.saveResult(Optional.<Integer> absent(), Optional.<String> absent(), Optional.of(message));
  }
  
  public boolean shouldHealthcheck(final SingularityTask task) {
    if (task.getTaskRequest().getRequest().isScheduled() || !task.getTaskRequest().getDeploy().getHealthcheckUri().isPresent()) {
      return false;
    }
    
    return true;
  }
  
  private void asyncHealthcheck(final SingularityTask task) {
    final SingularityHealthcheckAsyncHandler handler = new SingularityHealthcheckAsyncHandler(configuration, this, taskManager, abort, task);
    final Optional<String> uri = getHealthcheckUri(task);
    
    if (!uri.isPresent()) {
      saveFailure(handler, "Invalid healthcheck uri or ports not present");
      return;
    }
    
    final Long timeoutSeconds = task.getTaskRequest().getDeploy().getHealthcheckTimeoutSeconds().or(configuration.getDefaultHealthcheckTimeoutSeconds());
    
    try {
      PerRequestConfig prc = new PerRequestConfig();
      prc.setRequestTimeoutInMs((int) TimeUnit.SECONDS.toMillis(timeoutSeconds));
      
      RequestBuilder builder = new RequestBuilder("GET");
      builder.setUrl(uri.get());
      builder.setPerRequestConfig(prc);
      
      LOG.trace("Issuing a healthcheck ({}) for task {} with timeout {}s", uri.get(), task.getTaskId(), timeoutSeconds);
      
      http.prepareRequest(builder.build()).execute(handler);
    } catch (Throwable t) {
      LOG.debug("Exception while preparing healthcheck ({}) for task ({})", uri, task.getTaskId(), t);
      
      saveFailure(handler, String.format("Healthcheck failed due to exception: %s", t.getMessage()));
    }
  }
  
}
