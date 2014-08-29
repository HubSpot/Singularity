package com.hubspot.singularity.data;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityWebhook;
import com.hubspot.singularity.WebhookType;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.SingularityRequestHistoryTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskHistoryUpdateTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityWebhookTranscoder;

public class WebhookManager extends CuratorAsyncManager {

  private static final String ROOT_PATH = "/hooks";
  private static final String QUEUES_PATH = ROOT_PATH + "/queues";
  private static final String ACTIVE_PATH = ROOT_PATH + "/active";

  private final SingularityWebhookTranscoder webhookTranscoder;
  private final SingularityRequestHistoryTranscoder requestHistoryTranscoder;
  private final SingularityTaskHistoryUpdateTranscoder taskHistoryUpdateTranscoder;
  
  @Inject
  public WebhookManager(SingularityConfiguration configuration, CuratorFramework curator, SingularityWebhookTranscoder webhookTranscoder, SingularityRequestHistoryTranscoder requestHistoryTranscoder, SingularityTaskHistoryUpdateTranscoder taskHistoryUpdateTranscoder) {
    super(curator, configuration.getZookeeperAsyncTimeout());
    this.webhookTranscoder = webhookTranscoder;
    this.taskHistoryUpdateTranscoder = taskHistoryUpdateTranscoder;
    this.requestHistoryTranscoder = requestHistoryTranscoder;
  }

  public List<SingularityWebhook> getActiveWebhooks() {
    return getAsyncChildren(ACTIVE_PATH, webhookTranscoder);
  }
  
  public Iterable<SingularityWebhook> getActiveWebhooksByType(final WebhookType type) {
    return Iterables.filter(getActiveWebhooks(), new Predicate<SingularityWebhook>() {

      @Override
      public boolean apply(SingularityWebhook input) {
        return input.getType() == type;
      }
    
    });
  }

  private String getTaskHistoryUpdateId(SingularityTaskHistoryUpdate taskUpdate) {
    return taskUpdate.getTaskId() + "-" + taskUpdate.getTaskState().name();
  }
  
  private String getRequestHistoryUpdateId(SingularityRequestHistory requestUpdate) {
    return requestUpdate.getRequest().getId() + "-" + requestUpdate.getEventType().name() + "-" + requestUpdate.getCreatedAt();
  }

  private String getWebhookPath(String webhookId) {
    return ZKPaths.makePath(ACTIVE_PATH, webhookId);
  }

  private String getEnqueuePathForWebhook(String webhookId, WebhookType type) {
    return ZKPaths.makePath(ZKPaths.makePath(QUEUES_PATH, type.name()), webhookId);
  }
  
  private String getEnqueuePathForRequestUpdate(String webhookId, SingularityRequestHistory requestUpdate) {
    return ZKPaths.makePath(getEnqueuePathForWebhook(webhookId, WebhookType.REQUEST), getRequestHistoryUpdateId(requestUpdate));
  }
  
  private String getEnqueuePathForTaskUpdate(String webhookId, SingularityTaskHistoryUpdate taskUpdate) {
    return ZKPaths.makePath(getEnqueuePathForWebhook(webhookId, WebhookType.TASK), getTaskHistoryUpdateId(taskUpdate));
  }

  public SingularityCreateResult addWebhook(SingularityWebhook webhook) {
    final String path = getWebhookPath(webhook.getId());

    return create(path, webhook, webhookTranscoder);
  }

  public SingularityDeleteResult deleteWebhook(String webhookId) {
    final String path = getWebhookPath(webhookId);

    return delete(path);
  }

  public SingularityDeleteResult deleteTaskUpdate(SingularityWebhook webhook, SingularityTaskHistoryUpdate taskUpdate) {
    final String path = getEnqueuePathForTaskUpdate(webhook.getId(), taskUpdate);
    
    return delete(path);
  }
  
  public SingularityDeleteResult deleteRequestUpdate(SingularityWebhook webhook, SingularityRequestHistory requestUpdate) {
    final String path = getEnqueuePathForRequestUpdate(webhook.getId(), requestUpdate);
    
    return delete(path);
  }
  
  public List<SingularityTaskHistoryUpdate> getQueuedTaskUpdatesForHook(String webhookId) {
    return getAsyncChildren(getEnqueuePathForWebhook(webhookId, WebhookType.TASK), taskHistoryUpdateTranscoder);
  }
  
  public List<SingularityRequestHistory> getQueuedRequestHistoryForHook(String webhookId) {
    return getAsyncChildren(getEnqueuePathForWebhook(webhookId, WebhookType.REQUEST), requestHistoryTranscoder);
  }

  //TODO consider caching the list of hooks (at the expense of needing to refresh the cache and not immediately make some webhooks)
  public void enqueueRequestUpdate(SingularityRequestHistory requestUpdate) {
    for (SingularityWebhook webhook : getActiveWebhooksByType(WebhookType.REQUEST)) {
      final String enqueuePath = getEnqueuePathForRequestUpdate(webhook.getId(), requestUpdate);

      save(enqueuePath, requestUpdate, requestHistoryTranscoder);
    }
  }
  
  public void enqueueTaskUpdate(SingularityTaskHistoryUpdate taskUpdate) {
    for (SingularityWebhook webhook : getActiveWebhooksByType(WebhookType.TASK)) {
      final String enqueuePath = getEnqueuePathForTaskUpdate(webhook.getId(), taskUpdate);

      save(enqueuePath, taskUpdate, taskHistoryUpdateTranscoder);
    }
  }
  

}
