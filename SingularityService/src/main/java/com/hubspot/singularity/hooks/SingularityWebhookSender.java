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
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskWebhook;
import com.hubspot.singularity.SingularityWebhook;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.WebhookManager;
import com.hubspot.singularity.data.history.HistoryManager;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;

public class SingularityWebhookSender {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityWebhookSender.class);

  private final AsyncHttpClient http;
  private final WebhookManager webhookManager;
  private final TaskManager taskManager;
  private final HistoryManager historyManager;
  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityWebhookSender(AsyncHttpClient http, HistoryManager historyManager, ObjectMapper objectMapper, TaskManager taskManager, WebhookManager webhookManager) {
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
    
    for (SingularityWebhook webhook : webhooks) {
      
      switch (webhook.getType()) {
      case TASK:
        taskUpdates += checkTaskUpdates(webhook);
      case REQUEST:
        requestUpdates += checkRequestUpdates(webhook);
      }
    }

    LOG.info("Sent {} task updates, {} request updates for {} webhooks in {}", taskUpdates, requestUpdates, webhooks.size(), JavaUtils.duration(start));
  }
  
  private int checkRequestUpdates(SingularityWebhook webhook) {
    return 0;
  }
  
  private int checkTaskUpdates(SingularityWebhook webhook) {
    List<SingularityTaskHistoryUpdate> taskUpdates = webhookManager.getQueuedTaskUpdatesForHook(webhook.getId());

    for (SingularityTaskHistoryUpdate taskUpdate : taskUpdates) {
      Optional<SingularityTask> task = getTask(taskUpdate.getTaskId());
      
      // TODO compress 
      if (!task.isPresent()) {
        LOG.warn("Couldn't find task for taskUpdate {}", taskUpdate);
        webhookManager.deleteTaskUpdate(webhook, taskUpdate);
      }
      
      LOG.trace("Sending {} to {}", taskUpdate, webhook.getUri());
      
      BoundRequestBuilder postRequest = http.preparePost(webhook.getUri());

      try {
        postRequest.setBody(objectMapper.writeValueAsBytes(new SingularityTaskWebhook(task.get(), taskUpdate)));
      } catch (JsonProcessingException e) {
        throw Throwables.propagate(e);
      }

      // TODO handle retries, errors.
      try {
        postRequest.execute(new SingularityTaskWebhookAsyncHandler(webhookManager, webhook, taskUpdate));
      } catch (IOException e) {
        // this is probably a retry
        LOG.warn("Couldn't execute webhook to {}", webhook.getUri(), e);
      }
    }

    return taskUpdates.size();
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
