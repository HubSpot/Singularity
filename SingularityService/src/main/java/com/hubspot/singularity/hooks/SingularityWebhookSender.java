package com.hubspot.singularity.hooks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
import com.hubspot.singularity.SingularityDeployUpdate;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskWebhook;
import com.hubspot.singularity.SingularityWebhook;
import com.hubspot.singularity.async.AsyncSemaphore;
import com.hubspot.singularity.async.CompletableFutures;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.WebhookManager;
import com.hubspot.singularity.data.history.TaskHistoryHelper;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Response;

@Singleton
public class SingularityWebhookSender {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityWebhookSender.class);

  private final SingularityConfiguration configuration;
  private final AsyncHttpClient http;
  private final WebhookManager webhookManager;
  private final TaskHistoryHelper taskHistoryHelper;
  private final ObjectMapper objectMapper;

  private final AsyncSemaphore<Response> webhookSemaphore;

  @Inject
  public SingularityWebhookSender(SingularityConfiguration configuration, AsyncHttpClient http, ObjectMapper objectMapper, TaskHistoryHelper taskHistoryHelper, WebhookManager webhookManager) {
    this.configuration = configuration;
    this.http = http;
    this.webhookManager = webhookManager;
    this.taskHistoryHelper = taskHistoryHelper;
    this.objectMapper = objectMapper;

    this.webhookSemaphore = AsyncSemaphore.newBuilder(configuration::getMaxConcurrentWebhooks).build();
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

    List<CompletableFuture<Response>> webhookFutures = new ArrayList<>();

    for (SingularityWebhook webhook : webhooks) {
      switch (webhook.getType()) {
        case TASK:
          taskUpdates += checkTaskUpdates(webhook, webhookFutures);
          break;
        case REQUEST:
          requestUpdates += checkRequestUpdates(webhook, webhookFutures);
          break;
        case DEPLOY:
          deployUpdates += checkDeployUpdates(webhook, webhookFutures);
          break;
        default:
          break;
      }
    }

    CompletableFutures.allOf(webhookFutures)
        .exceptionally((t) -> {
          LOG.error("Exception in webhook", t);
          return null;
        });

    LOG.info("Sent {} task, {} request, and {} deploy updates for {} webhooks in {}", taskUpdates, requestUpdates, deployUpdates, webhooks.size(), JavaUtils.duration(start));
  }

  private boolean shouldDeleteUpdateOnFailure(int numUpdates, long updateTimestamp) {
    if (configuration.getMaxQueuedUpdatesPerWebhook() > 0 && numUpdates > configuration.getMaxQueuedUpdatesPerWebhook()) {
      return true;
    }
    final long updateAge = System.currentTimeMillis() - updateTimestamp;
    return configuration.getDeleteUndeliverableWebhooksAfterHours() > 0
        && updateAge > TimeUnit.HOURS.toMillis(configuration.getDeleteUndeliverableWebhooksAfterHours());
  }

  private int checkRequestUpdates(SingularityWebhook webhook, List<CompletableFuture<Response>> webhookFutures) {
    final List<SingularityRequestHistory> requestUpdates = webhookManager.getQueuedRequestHistoryForHook(webhook.getId());

    int numRequestUpdates = 0;

    for (SingularityRequestHistory requestUpdate : requestUpdates) {
      String concreteUri = applyPlaceholders(webhook.getUri(), requestUpdate);
      webhookFutures.add(webhookSemaphore.call(() ->
          executeWebhookAsync(
              concreteUri,
              requestUpdate,
              new SingularityRequestWebhookAsyncHandler(webhookManager, webhook, requestUpdate, shouldDeleteUpdateOnFailure(numRequestUpdates, requestUpdate.getCreatedAt())))
      ));
    }

    return requestUpdates.size();
  }

  private int checkDeployUpdates(SingularityWebhook webhook, List<CompletableFuture<Response>> webhookFutures) {
    final List<SingularityDeployUpdate> deployUpdates = webhookManager.getQueuedDeployUpdatesForHook(webhook.getId());

    int numDeployUpdates = 0;

    for (SingularityDeployUpdate deployUpdate : deployUpdates) {
      String concreteUri = applyPlaceholders(webhook.getUri(), deployUpdate);
      webhookFutures.add(webhookSemaphore.call(() ->
          executeWebhookAsync(
              concreteUri,
              deployUpdate,
              new SingularityDeployWebhookAsyncHandler(webhookManager, webhook, deployUpdate, shouldDeleteUpdateOnFailure(numDeployUpdates, deployUpdate.getDeployMarker().getTimestamp())))
      ));
    }

    return deployUpdates.size();
  }

  private int checkTaskUpdates(SingularityWebhook webhook, List<CompletableFuture<Response>> webhookFutures) {
    final List<SingularityTaskHistoryUpdate> taskUpdates = webhookManager.getQueuedTaskUpdatesForHook(webhook.getId());

    int numTaskUpdates = 0;

    for (SingularityTaskHistoryUpdate taskUpdate : taskUpdates) {
      Optional<SingularityTask> task = taskHistoryHelper.getTask(taskUpdate.getTaskId());

      // TODO compress
      if (!task.isPresent()) {
        LOG.warn("Couldn't find task for taskUpdate {}", taskUpdate);
        webhookManager.deleteTaskUpdate(webhook, taskUpdate);
        continue;
      }

      String concreteUri = applyPlaceholders(webhook.getUri(), taskUpdate);
      webhookFutures.add(webhookSemaphore.call(() ->
          executeWebhookAsync(
              concreteUri,
              new SingularityTaskWebhook(task.get(), taskUpdate),
              new SingularityTaskWebhookAsyncHandler(webhookManager, webhook, taskUpdate, shouldDeleteUpdateOnFailure(numTaskUpdates, taskUpdate.getTimestamp())))
      ));
    }

    return taskUpdates.size();
  }

  private String applyPlaceholders(String uri, SingularityRequestHistory requestHistory) {
    return uri
        .replaceAll("\\$REQUEST_ID", requestHistory.getRequest().getId());
  }

  private String applyPlaceholders(String uri, SingularityDeployUpdate deployUpdate) {
    return uri
        .replaceAll("\\$REQUEST_ID", deployUpdate.getDeployMarker().getRequestId())
        .replaceAll("\\$DEPLOY_ID", deployUpdate.getDeployMarker().getDeployId());
  }

  private String applyPlaceholders(String uri, SingularityTaskHistoryUpdate taskUpdate) {
    return uri
        .replaceAll("\\$REQUEST_ID", taskUpdate.getTaskId().getRequestId())
        .replaceAll("\\$DEPLOY_ID", taskUpdate.getTaskId().getDeployId())
        .replaceAll("\\$TASK_ID", taskUpdate.getTaskId().getId());

  }

  // TODO handle retries, errors.
  private <T> CompletableFuture<Response> executeWebhookAsync(String uri, Object payload, AbstractSingularityWebhookAsyncHandler<T> handler) {
    LOG.trace("Sending {} to {}", payload, uri);
    BoundRequestBuilder postRequest = http.preparePost(uri);
    postRequest.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

    try {
      postRequest.setBody(objectMapper.writeValueAsBytes(payload));
    } catch (JsonProcessingException e) {
      throw Throwables.propagate(e);
    }

    CompletableFuture<Response> webhookFuture = new CompletableFuture<>();
    try {
      handler.setCompletableFuture(webhookFuture);
      postRequest.execute(handler);
    } catch (IOException e) {
      LOG.warn("Couldn't execute webhook to {}", uri, e);

      if (handler.shouldDeleteUpdateDueToQueueAboveCapacity()) {
        handler.deleteWebhookUpdate();
      }
      webhookFuture.completeExceptionally(e);
    }
    return webhookFuture;
  }


}
