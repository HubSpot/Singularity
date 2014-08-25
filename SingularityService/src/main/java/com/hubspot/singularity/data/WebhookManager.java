package com.hubspot.singularity.data;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityWebhook;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.SingularityTaskHistoryUpdateTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityWebhookTranscoder;

public class WebhookManager extends CuratorAsyncManager {

  private static final String ROOT_PATH = "/hooks";
  private static final String QUEUES_PATH = ROOT_PATH + "/queues";
  private static final String ACTIVE_PATH = ROOT_PATH + "/active";

  private final SingularityWebhookTranscoder webhookTranscoder;
  private final SingularityTaskHistoryUpdateTranscoder taskHistoryUpdateTranscoder;

  @Inject
  public WebhookManager(SingularityConfiguration configuration, CuratorFramework curator, SingularityWebhookTranscoder webhookTranscoder, SingularityTaskHistoryUpdateTranscoder taskHistoryUpdateTranscoder) {
    super(curator, configuration.getZookeeperAsyncTimeout());
    this.webhookTranscoder = webhookTranscoder;
    this.taskHistoryUpdateTranscoder = taskHistoryUpdateTranscoder;
  }

  public List<SingularityWebhook> getActiveWebhooks() {
    return getAsyncChildren(ACTIVE_PATH, webhookTranscoder);
  }

  private String getTaskHistoryUpdateId(SingularityTaskHistoryUpdate taskUpdate) {
    return taskUpdate.getTaskId() + "-" + taskUpdate.getTaskState().name();
  }

  private String getWebhookPath(String webhookId) {
    return ZKPaths.makePath(ACTIVE_PATH, webhookId);
  }

  private String getEnqueuePathForWebhook(String webhookId) {
    return ZKPaths.makePath(QUEUES_PATH, webhookId);
  }

  private String getEnqueuePathForTaskUpdate(String webhookId, SingularityTaskHistoryUpdate taskUpdate) {
    return ZKPaths.makePath(getEnqueuePathForWebhook(webhookId), getTaskHistoryUpdateId(taskUpdate));
  }

  public SingularityCreateResult addWebhook(SingularityWebhook webhook) {
    final String path = getWebhookPath(webhook.getId());

    return create(path, webhook, webhookTranscoder);
  }

  public SingularityDeleteResult deleteWebhook(String webhookId) {
    final String path = getWebhookPath(webhookId);

    return delete(path);
  }

  public List<SingularityTaskHistoryUpdate> getQueuedUpdatesForHook(String webhookId) {
    return getAsyncChildren(getEnqueuePathForWebhook(webhookId), taskHistoryUpdateTranscoder);
  }

  public void enqueue(SingularityTaskHistoryUpdate taskUpdate) {
    // TODO consider caching the list of hooks (at the expense of needing to refresh the cache and not immediately make some webhooks)
    for (SingularityWebhook webhook : getActiveWebhooks()) {
      final String enqueuePath = getEnqueuePathForTaskUpdate(webhook.getId(), taskUpdate);

      save(enqueuePath, taskUpdate, taskHistoryUpdateTranscoder);
    }
  }

}
