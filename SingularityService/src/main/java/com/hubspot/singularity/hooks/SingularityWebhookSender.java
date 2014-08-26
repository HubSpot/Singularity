package com.hubspot.singularity.hooks;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityCloseable;
import com.hubspot.singularity.SingularityCloser;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskWebhook;
import com.hubspot.singularity.SingularityWebhook;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.WebhookManager;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;

public class SingularityWebhookSender implements SingularityCloseable {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityWebhookSender.class);

  private final AsyncHttpClient http;
  private final WebhookManager webhookManager;
  private final SingularityExceptionNotifier exceptionNotifier;
  private final SingularityConfiguration configuration;
  private final SingularityCloser closer;
  private final TaskManager taskManager;
  private final HistoryManager historyManager;
  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityWebhookSender(AsyncHttpClient http, SingularityCloser closer, HistoryManager historyManager, ObjectMapper objectMapper, TaskManager taskManager, 
      SingularityExceptionNotifier exceptionNotifier, WebhookManager webhookManager, SingularityConfiguration configuration) {
    this.http = http;
    this.closer = closer;
    this.webhookManager = webhookManager;
    this.configuration = configuration;
    this.exceptionNotifier = exceptionNotifier;
    this.taskManager = taskManager;
    this.objectMapper = objectMapper;
    this.historyManager = historyManager;
  }

  public void checkWebhooks() {
    final long start = System.currentTimeMillis();

    final List<SingularityWebhook> webhooks = webhookManager.getActiveWebhooks();
    if (webhooks.isEmpty()) {
      return;
    }

    int numUpdates = 0;

    for (SingularityWebhook webhook : webhooks) {
      List<SingularityTaskHistoryUpdate> taskUpdates = webhookManager.getQueuedUpdatesForHook(webhook.getId());

      for (SingularityTaskHistoryUpdate taskUpdate : taskUpdates) {
        Optional<SingularityTask> task = getTask(taskUpdate.getTaskId());
        
        // TODO compress 
        if (task.isPresent()) {
          BoundRequestBuilder postRequest = http.preparePost(webhook.getUri());
          
          try {
            postRequest.setBody(objectMapper.writeValueAsBytes(new SingularityTaskWebhook(task.get(), taskUpdate)));
          } catch (JsonProcessingException e) {
            throw Throwables.propagate(e);
          }
          
          try {
            postRequest.execute(new SingularityWebhookAsyncHandler());
          } catch (IOException e) {
            // this is probably a retry
          }
          
        }
      }
      

    }

    LOG.info("Sent {} updates for {} webhooks in {}", numUpdates, webhooks.size(), JavaUtils.duration(start));
  }
  
  // TODO cache this?
  private Optional<SingularityTask> getTask(SingularityTaskId taskId) {
    Optional<SingularityTask> maybeTask = taskManager.getTask(taskId);
    
    if (maybeTask.isPresent()) {
      return maybeTask;
    }
    
    Optional<SingularityTaskHistory> history = historyManager.getTaskHistory(taskId.getId());
      
    if (history.isPresent()) {
      return Optional.of(history.get().getTask());
    }

    return Optional.absent();
  }
  
  public void close() {
    // closer.shutdown(getClass().getName(), executorService, 1);
  }

}
