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
import com.hubspot.singularity.SingularityRequestHistory;
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
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;

public class SingularityWebhookSender {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityWebhookSender.class);

  private final SingularityConfiguration configuration;
  private final AsyncHttpClient http;
  private final WebhookManager webhookManager;
  private final TaskManager taskManager;
  private final HistoryManager historyManager;
  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityWebhookSender(SingularityConfiguration configuration, AsyncHttpClient http, HistoryManager historyManager, ObjectMapper objectMapper, TaskManager taskManager, WebhookManager webhookManager) {
    this.configuration = configuration;
    this.http = http;
    this.webhookManager = webhookManager;
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

    int taskUpdates = 0;
    int requestUpdates = 0;
    int deployUpdates = 0;
    
    for (SingularityWebhook webhook : webhooks) {
      
      switch (webhook.getType()) {
      case TASK:
        taskUpdates += checkTaskUpdates(webhook);
      case REQUEST:
        requestUpdates += checkRequestUpdates(webhook);
      case DEPLOY:
        deployUpdates += checkDeployUpdates(webhook);
      }
    }

    LOG.info("Sent {} task, {} request, and {} deploy updates for {} webhooks in {}", taskUpdates, requestUpdates, deployUpdates, webhooks.size(), JavaUtils.duration(start));
  }
  
  private int checkRequestUpdates(SingularityWebhook webhook) {
    final List<SingularityRequestHistory> requestUpdates = webhookManager.getQueuedRequestHistoryForHook(webhook.getId());
    
    int numRequestUpdates = 0;
    
    for (SingularityRequestHistory requestUpdate : requestUpdates) {
      executeWebhook(webhook, requestUpdate, new SingularityRequestWebhookAsyncHandler(webhookManager, webhook, requestUpdate, numRequestUpdates++ > configuration.getMaxQueuedUpdatesPerWebhook()));
    }
    
    return requestUpdates.size();
  }
  
  private int checkDeployUpdates(SingularityWebhook webhook) {
    return 0;
  }
  
  private int checkTaskUpdates(SingularityWebhook webhook) {
    final List<SingularityTaskHistoryUpdate> taskUpdates = webhookManager.getQueuedTaskUpdatesForHook(webhook.getId());

    int numTaskUpdates = 0;
    
    for (SingularityTaskHistoryUpdate taskUpdate : taskUpdates) {
      Optional<SingularityTask> task = getTask(taskUpdate.getTaskId());
      
      // TODO compress 
      if (!task.isPresent()) {
        LOG.warn("Couldn't find task for taskUpdate {}", taskUpdate);
        webhookManager.deleteTaskUpdate(webhook, taskUpdate);
      }
      
      executeWebhook(webhook, new SingularityTaskWebhook(task.get(), taskUpdate), new SingularityTaskWebhookAsyncHandler(webhookManager, webhook, taskUpdate, numTaskUpdates++ > configuration.getMaxQueuedUpdatesPerWebhook()));
    }

    return taskUpdates.size();
  }
  
  // TODO handle retries, errors.
  private <T> void executeWebhook(SingularityWebhook webhook, Object payload, AbstractSingularityWebhookAsyncHandler<T> handler) {
    LOG.trace("Sending {} to {}", payload, webhook.getUri());
    
    BoundRequestBuilder postRequest = http.preparePost(webhook.getUri());

    try {
      postRequest.setBody(objectMapper.writeValueAsBytes(payload));
    } catch (JsonProcessingException e) {
      throw Throwables.propagate(e);
    }
    
    try {
      postRequest.execute(handler);
    } catch (IOException e) {
      LOG.warn("Couldn't execute webhook to {}", webhook.getUri(), e);
      
      if (handler.shouldDeleteUpdateDueToQueueAboveCapacity()) {
        handler.deleteWebhookUpdate();
      }
    }
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

}
