package com.hubspot.singularity.data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.CrashLoopInfo;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityDeployUpdate;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityWebhook;
import com.hubspot.singularity.SingularityWebhookSummary;
import com.hubspot.singularity.WebhookType;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;

@Singleton
public class WebhookManager extends CuratorAsyncManager {
  private static final Logger LOG = LoggerFactory.getLogger(WebhookManager.class);

  private static final String ROOT_PATH = "/hooks";
  private static final String QUEUES_PATH = ROOT_PATH + "/queues";
  private static final String ACTIVE_PATH = ROOT_PATH + "/active";

  private static final String SNS_RETRY_ROOT = ROOT_PATH + "/snsretry";
  private static final String SNS_TASK_RETRY_ROOT = SNS_RETRY_ROOT + "/task";
  private static final String SNS_REQUEST_RETRY = SNS_RETRY_ROOT + "/request";
  private static final String SNS_DEPLOY_RETRY = SNS_RETRY_ROOT + "/deploy";
  private static final String SNS_CRASH_LOOP_RETRY = SNS_RETRY_ROOT + "/crashloop";

  private final Transcoder<SingularityWebhook> webhookTranscoder;
  private final Transcoder<SingularityRequestHistory> requestHistoryTranscoder;
  private final Transcoder<SingularityTaskHistoryUpdate> taskHistoryUpdateTranscoder;
  private final Transcoder<SingularityDeployUpdate> deployWebhookTranscoder;
  private final Transcoder<CrashLoopInfo> crashLoopUpdateTranscoder;

  private final Cache<WebhookType, List<SingularityWebhook>> activeWebhooksCache;
  private final Cache<String, Integer> childNodeCountCache;
  private final int maxChildNodes;

  @Inject
  public WebhookManager(CuratorFramework curator, SingularityConfiguration configuration, MetricRegistry metricRegistry, Transcoder<SingularityWebhook> webhookTranscoder,
                        Transcoder<SingularityRequestHistory> requestHistoryTranscoder, Transcoder<SingularityTaskHistoryUpdate> taskHistoryUpdateTranscoder, Transcoder<SingularityDeployUpdate> deployWebhookTranscoder,
                        Transcoder<CrashLoopInfo> crashLoopUpdateTranscoder) {
    super(curator, configuration, metricRegistry);

    this.webhookTranscoder = webhookTranscoder;
    this.taskHistoryUpdateTranscoder = taskHistoryUpdateTranscoder;
    this.requestHistoryTranscoder = requestHistoryTranscoder;
    this.deployWebhookTranscoder = deployWebhookTranscoder;
    this.crashLoopUpdateTranscoder = crashLoopUpdateTranscoder;

    this.activeWebhooksCache = CacheBuilder.newBuilder()
        .expireAfterWrite(60, TimeUnit.SECONDS)
        .build();
    this.childNodeCountCache = CacheBuilder.newBuilder()
        .expireAfterWrite(5, TimeUnit.SECONDS)
        .build();
    this.maxChildNodes = configuration.getWebhookQueueConfiguration().getMaxZkQueuedWebhooksPerParentNode();
  }

  public List<SingularityWebhook> getActiveWebhooksUncached() {
    return getAsyncChildren(ACTIVE_PATH, webhookTranscoder);
  }

  public Iterable<SingularityWebhook> getActiveWebhooksByType(final WebhookType type) {
    List<SingularityWebhook> maybeCached = activeWebhooksCache.getIfPresent(type);
    if (maybeCached != null) {
      return maybeCached;
    } else {
      List<SingularityWebhook> webhooks = getActiveWebhooksUncached();
      List<SingularityWebhook> forType = webhooks.stream()
          .filter((w) -> w.getType() == type)
          .collect(Collectors.toList());
      activeWebhooksCache.put(type, forType);
      return forType;
    }
  }

  private String getTaskHistoryUpdateId(SingularityTaskHistoryUpdate taskUpdate) {
    return taskUpdate.getTaskId() + "-" + taskUpdate.getTaskState().name();
  }

  private String getRequestHistoryUpdateId(SingularityRequestHistory requestUpdate) {
    return requestUpdate.getRequest().getId() + "-" + requestUpdate.getEventType().name() + "-" + requestUpdate.getCreatedAt();
  }

  private String getDeployUpdateId(SingularityDeployUpdate deployUpdate) {
    return SingularityDeployKey.fromDeployMarker(deployUpdate.getDeployMarker()) + "-" + deployUpdate.getEventType().name();
  }

  private String getCrashLoopUpdateId(CrashLoopInfo crashLoopUpdate) {
    return String.format("%s-%s-%s-%s", crashLoopUpdate.getRequestId(), crashLoopUpdate.getDeployId(), crashLoopUpdate.getType(), crashLoopUpdate.getStart());
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

  private String getEnqueuePathForDeployUpdate(String webhookId, SingularityDeployUpdate deployUpdate) {
    return ZKPaths.makePath(getEnqueuePathForWebhook(webhookId, WebhookType.DEPLOY), getDeployUpdateId(deployUpdate));
  }

  private String getEnqueuePathForCrashLoopUpdate(String webhookId, CrashLoopInfo crashLoopUpdate) {
    return ZKPaths.makePath(getEnqueuePathForWebhook(webhookId, WebhookType.CRASHLOOP), getCrashLoopUpdateId(crashLoopUpdate));
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

  public SingularityDeleteResult deleteDeployUpdate(SingularityWebhook webhook, SingularityDeployUpdate deployUpdate) {
    final String path = getEnqueuePathForDeployUpdate(webhook.getId(), deployUpdate);

    return delete(path);
  }

  public SingularityDeleteResult deleteCrashLoopUpdate(SingularityWebhook webhook, CrashLoopInfo crashLoopUpdate) {
    final String path = getEnqueuePathForCrashLoopUpdate(webhook.getId(), crashLoopUpdate);

    return delete(path);
  }

  public SingularityDeleteResult deleteRequestUpdate(SingularityWebhook webhook, SingularityRequestHistory requestUpdate) {
    final String path = getEnqueuePathForRequestUpdate(webhook.getId(), requestUpdate);

    return delete(path);
  }

  public List<SingularityDeployUpdate> getQueuedDeployUpdatesForHook(String webhookId) {
    return getAsyncChildren(getEnqueuePathForWebhook(webhookId, WebhookType.DEPLOY), deployWebhookTranscoder);
  }

  public List<CrashLoopInfo> getQueuedCrashLoopUpdatesForHook(String webhookId) {
    return getAsyncChildren(getEnqueuePathForWebhook(webhookId, WebhookType.CRASHLOOP), crashLoopUpdateTranscoder);
  }

  public List<SingularityTaskHistoryUpdate> getQueuedTaskUpdatesForHook(String webhookId) {
    return getAsyncChildren(getEnqueuePathForWebhook(webhookId, WebhookType.TASK), taskHistoryUpdateTranscoder);
  }

  public List<SingularityRequestHistory> getQueuedRequestHistoryForHook(String webhookId) {
    return getAsyncChildren(getEnqueuePathForWebhook(webhookId, WebhookType.REQUEST), requestHistoryTranscoder);
  }

  public List<SingularityWebhookSummary> getWebhooksWithQueueSize() {
    List<SingularityWebhookSummary> webhooks = new ArrayList<>();
    for (SingularityWebhook webhook : getActiveWebhooksUncached()) {
      webhooks.add(new SingularityWebhookSummary(webhook, getNumChildren(getEnqueuePathForWebhook(webhook.getId(), webhook.getType()))));
    }
    return webhooks;
  }

  void saveRequestHistoryEvent(SingularityRequestHistory requestUpdate) {
    for (SingularityWebhook webhook : getActiveWebhooksByType(WebhookType.REQUEST)) {
      String parentPath = getEnqueuePathForWebhook(webhook.getId(), WebhookType.REQUEST);
      if (!isChildNodeCountSafe(parentPath)) {
        LOG.warn("Too many queued webhooks for path {}, dropping", parentPath);
        return;
      }
      final String enqueuePath = getEnqueuePathForRequestUpdate(webhook.getId(), requestUpdate);
      save(enqueuePath, requestUpdate, requestHistoryTranscoder);
    }
  }

  void saveTaskHistoryUpdateEvent(SingularityTaskHistoryUpdate taskUpdate) {
    for (SingularityWebhook webhook : getActiveWebhooksByType(WebhookType.TASK)) {
      String parentPath = getEnqueuePathForWebhook(webhook.getId(), WebhookType.TASK);
      if (!isChildNodeCountSafe(parentPath)) {
        LOG.warn("Too many queued webhooks for path {}, dropping", parentPath);
        return;
      }
      final String enqueuePath = getEnqueuePathForTaskUpdate(webhook.getId(), taskUpdate);
      save(enqueuePath, taskUpdate, taskHistoryUpdateTranscoder);
    }
  }

  void saveDeployHistoryEvent(SingularityDeployUpdate deployUpdate) {
    for (SingularityWebhook webhook : getActiveWebhooksByType(WebhookType.DEPLOY)) {
      String parentPath = getEnqueuePathForWebhook(webhook.getId(), WebhookType.DEPLOY);
      if (!isChildNodeCountSafe(parentPath)) {
        LOG.warn("Too many queued webhooks for path {}, dropping", parentPath);
        return;
      }
      final String enqueuePath = getEnqueuePathForDeployUpdate(webhook.getId(), deployUpdate);
      save(enqueuePath, deployUpdate, deployWebhookTranscoder);
    }
  }

  void saveCrashLoopEvent(CrashLoopInfo crashLoopUpdate) {
    for (SingularityWebhook webhook : getActiveWebhooksByType(WebhookType.CRASHLOOP)) {
      String parentPath = getEnqueuePathForWebhook(webhook.getId(), WebhookType.CRASHLOOP);
      if (!isChildNodeCountSafe(parentPath)) {
        LOG.warn("Too many queued webhooks for path {}, dropping", parentPath);
        return;
      }
      final String enqueuePath = getEnqueuePathForCrashLoopUpdate(webhook.getId(), crashLoopUpdate);
      save(enqueuePath, crashLoopUpdate, crashLoopUpdateTranscoder);
    }
  }

  // Methods for use with sns poller
  public void saveTaskUpdateForRetry(SingularityTaskHistoryUpdate taskHistoryUpdate) {
    String parentPath = ZKPaths.makePath(SNS_TASK_RETRY_ROOT, taskHistoryUpdate.getTaskId().getRequestId());
    if (!isChildNodeCountSafe(parentPath)) {
      LOG.warn("Too many queued webhooks for path {}, dropping", parentPath);
      return;
    }
    String updatePath = ZKPaths.makePath(SNS_TASK_RETRY_ROOT, taskHistoryUpdate.getTaskId().getRequestId(), getTaskHistoryUpdateId(taskHistoryUpdate));
    save(updatePath, taskHistoryUpdate, taskHistoryUpdateTranscoder);
  }

  public void deleteTaskUpdateForRetry(SingularityTaskHistoryUpdate taskHistoryUpdate) {
    String updatePath = ZKPaths.makePath(SNS_TASK_RETRY_ROOT, taskHistoryUpdate.getTaskId().getRequestId(), getTaskHistoryUpdateId(taskHistoryUpdate));
    delete(updatePath);
  }

  public List<SingularityTaskHistoryUpdate> getTaskUpdatesToRetry() {
    List<SingularityTaskHistoryUpdate> results = new ArrayList<>();
    for (String requestId : getChildren(SNS_TASK_RETRY_ROOT)) {
      if (results.size() > configuration.getMaxConcurrentWebhooks()) {
        break;
      }
      results.addAll(getAsyncChildren(ZKPaths.makePath(SNS_TASK_RETRY_ROOT, requestId), taskHistoryUpdateTranscoder));
    }
    return results;
  }

  public void saveDeployUpdateForRetry(SingularityDeployUpdate deployUpdate) {
    if (!isChildNodeCountSafe(SNS_DEPLOY_RETRY)) {
      LOG.warn("Too many queued webhooks for path {}, dropping", SNS_DEPLOY_RETRY);
      return;
    }
    String updatePath = ZKPaths.makePath(SNS_DEPLOY_RETRY, getDeployUpdateId(deployUpdate));
    save(updatePath, deployUpdate, deployWebhookTranscoder);
  }

  public void deleteDeployUpdateForRetry(SingularityDeployUpdate deployUpdate) {
    String updatePath = ZKPaths.makePath(SNS_DEPLOY_RETRY, getDeployUpdateId(deployUpdate));
    delete(updatePath);
  }

  public List<SingularityDeployUpdate> getDeployUpdatesToRetry() {
    return getAsyncChildren(SNS_DEPLOY_RETRY, deployWebhookTranscoder);
  }

  public void saveCrashLoopUpdateForRetry(CrashLoopInfo crashLoopUpdate) {
    if (!isChildNodeCountSafe(SNS_CRASH_LOOP_RETRY)) {
      LOG.warn("Too many queued webhooks for path {}, dropping", SNS_CRASH_LOOP_RETRY);
      return;
    }
    String updatePath = ZKPaths.makePath(SNS_CRASH_LOOP_RETRY, getCrashLoopUpdateId(crashLoopUpdate));
    save(updatePath, crashLoopUpdate, crashLoopUpdateTranscoder);
  }

  public void deleteCrashLoopUpdateForRetry(CrashLoopInfo crashLoopUpdate) {
    String updatePath = ZKPaths.makePath(SNS_CRASH_LOOP_RETRY, getCrashLoopUpdateId(crashLoopUpdate));
    delete(updatePath);
  }

  public List<CrashLoopInfo> getCrashLoopUpdatesToRetry() {
    return getAsyncChildren(SNS_CRASH_LOOP_RETRY, crashLoopUpdateTranscoder);
  }

  public void saveRequestUpdateForRetry(SingularityRequestHistory requestHistory) {
    if (!isChildNodeCountSafe(SNS_REQUEST_RETRY)) {
      LOG.warn("Too many queued webhooks for path {}, dropping", SNS_REQUEST_RETRY);
      return;
    }
    String updatePath = ZKPaths.makePath(SNS_REQUEST_RETRY, getRequestHistoryUpdateId(requestHistory));
    save(updatePath, requestHistory, requestHistoryTranscoder);
  }

  public void deleteRequestUpdateForRetry(SingularityRequestHistory requestHistory) {
    String updatePath = ZKPaths.makePath(SNS_REQUEST_RETRY, getRequestHistoryUpdateId(requestHistory));
    delete(updatePath);
  }

  public List<SingularityRequestHistory> getRequestUpdatesToRetry() {
    return getAsyncChildren(SNS_REQUEST_RETRY, requestHistoryTranscoder);
  }

  private boolean isChildNodeCountSafe(String path) {
    Integer maybeCached = childNodeCountCache.getIfPresent(path);
    if (maybeCached != null){
      return maybeCached < maxChildNodes;
    }
    int count = getNumChildren(path);
    childNodeCountCache.put(path, count);
    return count < maxChildNodes;
  }
}
