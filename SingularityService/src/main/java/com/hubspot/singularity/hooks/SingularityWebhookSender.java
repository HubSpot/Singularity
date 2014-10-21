package com.hubspot.singularity.hooks;

import java.io.IOException;
import java.util.List;

import javax.inject.Singleton;
import javax.ws.rs.core.HttpHeaders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityDeployWebhook;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskWebhook;
import com.hubspot.singularity.SingularityWebhook;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.WebhookManager;
import com.hubspot.singularity.data.history.TaskHistoryHelper;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;

@Singleton
public class SingularityWebhookSender {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityWebhookSender.class);

  private final SingularityConfiguration configuration;
  private final AsyncHttpClient http;
  private final WebhookManager webhookManager;
  private final TaskHistoryHelper taskHistoryHelper;
  private final ObjectMapper objectMapper;

  @Inject
  public SingularityWebhookSender(SingularityConfiguration configuration, AsyncHttpClient http, ObjectMapper objectMapper, TaskHistoryHelper taskHistoryHelper, WebhookManager webhookManager) {
    this.configuration = configuration;
    this.http = http;
    this.webhookManager = webhookManager;
    this.taskHistoryHelper = taskHistoryHelper;
    this.objectMapper = objectMapper;
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
          break;
        case REQUEST:
          requestUpdates += checkRequestUpdates(webhook);
          break;
        case DEPLOY:
          deployUpdates += checkDeployUpdates(webhook);
          break;
        default:
          break;
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
    final List<SingularityDeployWebhook> deployUpdates = webhookManager.getQueuedDeployUpdatesForHook(webhook.getId());

    int numDeployUpdates = 0;

    for (SingularityDeployWebhook deployUpdate : deployUpdates) {
      executeWebhook(webhook, deployUpdate, new SingularityDeployWebhookAsyncHandler(webhookManager, webhook, deployUpdate, numDeployUpdates++ > configuration.getMaxQueuedUpdatesPerWebhook()));
    }

    return deployUpdates.size();
  }

  private int checkTaskUpdates(SingularityWebhook webhook) {
    final List<SingularityTaskHistoryUpdate> taskUpdates = webhookManager.getQueuedTaskUpdatesForHook(webhook.getId());

    int numTaskUpdates = 0;

    for (SingularityTaskHistoryUpdate taskUpdate : taskUpdates) {
      Optional<SingularityTask> task = taskHistoryHelper.getTask(taskUpdate.getTaskId());

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

    postRequest.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

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


}
