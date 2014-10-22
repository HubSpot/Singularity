package com.hubspot.singularity.data;

import java.util.Collection;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestCleanup.RequestCleanupType;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.SingularityPendingRequestTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityRequestCleanupTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityRequestHistoryTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityRequestWithStateTranscoder;

@Singleton
public class RequestManager extends CuratorAsyncManager {

  private static final Logger LOG = LoggerFactory.getLogger(RequestManager.class);

  private final SingularityRequestWithStateTranscoder requestTranscoder;
  private final SingularityPendingRequestTranscoder pendingRequestTranscoder;
  private final SingularityRequestCleanupTranscoder requestCleanupTranscoder;
  private final SingularityRequestHistoryTranscoder requestHistoryTranscoder;

  private final WebhookManager webhookManager;

  private static final String REQUEST_ROOT = "/requests";

  private static final String NORMAL_PATH_ROOT = REQUEST_ROOT + "/all";
  private static final String PENDING_PATH_ROOT = REQUEST_ROOT + "/pending";
  private static final String CLEANUP_PATH_ROOT = REQUEST_ROOT +  "/cleanup";
  private static final String HISTORY_PATH_ROOT = REQUEST_ROOT + "/history";

  @Inject
  public RequestManager(SingularityConfiguration configuration, CuratorFramework curator, WebhookManager webhookManager, SingularityRequestCleanupTranscoder requestCleanupTranscoder, SingularityRequestWithStateTranscoder requestTranscoder,
      SingularityPendingRequestTranscoder pendingRequestTranscoder, SingularityRequestHistoryTranscoder requestHistoryTranscoder) {
    super(curator, configuration.getZookeeperAsyncTimeout());

    this.requestTranscoder = requestTranscoder;
    this.requestCleanupTranscoder = requestCleanupTranscoder;
    this.pendingRequestTranscoder = pendingRequestTranscoder;
    this.requestHistoryTranscoder = requestHistoryTranscoder;
    this.webhookManager = webhookManager;
  }

  private String getRequestPath(String requestId) {
    return ZKPaths.makePath(NORMAL_PATH_ROOT, requestId);
  }

  private String getHistoryParentPath(String requestId) {
    return ZKPaths.makePath(HISTORY_PATH_ROOT, requestId);
  }

  private String getHistoryPath(SingularityRequestHistory history) {
    return ZKPaths.makePath(getHistoryParentPath(history.getRequest().getId()), history.getEventType() + "-" + history.getCreatedAt());
  }

  private String getPendingPath(String requestId, String deployId) {
    return ZKPaths.makePath(PENDING_PATH_ROOT, new SingularityDeployKey(requestId, deployId).getId());
  }

  private String getCleanupPath(String requestId) {
    return ZKPaths.makePath(CLEANUP_PATH_ROOT, requestId);
  }

  public int getSizeOfPendingQueue() {
    return getNumChildren(PENDING_PATH_ROOT);
  }

  public int getSizeOfCleanupQueue() {
    return getNumChildren(CLEANUP_PATH_ROOT);
  }

  public int getNumRequests() {
    return getNumChildren(NORMAL_PATH_ROOT);
  }

  public void deletePendingRequest(SingularityPendingRequest pendingRequest) {
    delete(getPendingPath(pendingRequest.getRequestId(), pendingRequest.getDeployId()));
  }

  public void deleteHistoryParent(String requestId) {
    delete(getHistoryParentPath(requestId));
  }

  public void deleteHistoryItem(SingularityRequestHistory history) {
    delete(getHistoryPath(history));
  }

  public void deleteCleanRequest(String requestId) {
    delete(getCleanupPath(requestId));
  }

  public List<String> getAllRequestIds() {
    return getChildren(NORMAL_PATH_ROOT);
  }

  public List<String> getRequestIdsWithHistory() {
    return getChildren(HISTORY_PATH_ROOT);
  }

  public List<SingularityRequestHistory> getRequestHistory(String requestId) {
    return getAsyncChildren(getHistoryParentPath(requestId), requestHistoryTranscoder);
  }

  public SingularityCreateResult createCleanupRequest(SingularityRequestCleanup cleanupRequest) {
    return create(getCleanupPath(cleanupRequest.getRequestId()), cleanupRequest, requestCleanupTranscoder);
  }

  private SingularityCreateResult save(SingularityRequest request, RequestState state, RequestHistoryType eventType, Optional<String> user) {
    saveHistory(new SingularityRequestHistory(System.currentTimeMillis(), user, eventType, request));

    return save(getRequestPath(request.getId()), new SingularityRequestWithState(request, state), requestTranscoder);
  }

  public SingularityCreateResult pause(SingularityRequest request, Optional<String> user) {
    return save(request, RequestState.PAUSED, RequestHistoryType.PAUSED, user);
  }

  public SingularityCreateResult cooldown(SingularityRequest request) {
    return save(request, RequestState.SYSTEM_COOLDOWN, RequestHistoryType.ENTERED_COOLDOWN, Optional.<String> absent());
  }

  public SingularityCreateResult finish(SingularityRequest request) {
    return save(request, RequestState.FINISHED, RequestHistoryType.FINISHED, Optional.<String> absent());
  }

  public SingularityCreateResult addToPendingQueue(SingularityPendingRequest pendingRequest) {
    SingularityCreateResult result = create(getPendingPath(pendingRequest.getRequestId(), pendingRequest.getDeployId()), pendingRequest, pendingRequestTranscoder);

    LOG.info("{} added to pending queue with result: {}", pendingRequest, result);

    return result;
  }

  @VisibleForTesting
  protected SingularityCreateResult saveHistory(SingularityRequestHistory history) {
    final String path = getHistoryPath(history);

    webhookManager.enqueueRequestUpdate(history);

    return save(path, history, requestHistoryTranscoder);
  }

  public SingularityCreateResult unpause(SingularityRequest request, Optional<String> user) {
    return activate(request, RequestHistoryType.UNPAUSED, user);
  }

  public SingularityCreateResult exitCooldown(SingularityRequest request) {
    return activate(request, RequestHistoryType.EXITED_COOLDOWN, Optional.<String> absent());
  }

  public SingularityCreateResult deployToUnpause(SingularityRequest request, Optional<String> user) {
    return save(request, RequestState.DEPLOYING_TO_UNPAUSE, RequestHistoryType.DEPLOYED_TO_UNPAUSE, user);
  }

  public SingularityCreateResult activate(SingularityRequest request, RequestHistoryType historyType, Optional<String> user) {
    return save(request, RequestState.ACTIVE, historyType, user);
  }

  public List<SingularityPendingRequest> getPendingRequests() {
    return getAsyncChildren(PENDING_PATH_ROOT, pendingRequestTranscoder);
  }

  public List<String> getCleanupRequestIds() {
    return getChildren(CLEANUP_PATH_ROOT);
  }

  public List<SingularityRequestCleanup> getCleanupRequests() {
    return getAsyncChildren(CLEANUP_PATH_ROOT, requestCleanupTranscoder);
  }

  public List<SingularityRequestWithState> getRequests(Collection<String> requestIds) {
    final List<String> paths = Lists.newArrayListWithCapacity(requestIds.size());
    for (String requestId : requestIds) {
      paths.add(getRequestPath(requestId));
    }

    return getAsync(RequestManager.NORMAL_PATH_ROOT, paths, requestTranscoder);
  }

  private Iterable<SingularityRequestWithState> filter(List<SingularityRequestWithState> requests, final RequestState... states) {
    return Iterables.filter(requests, new Predicate<SingularityRequestWithState>() {

      @Override
      public boolean apply(SingularityRequestWithState input) {
        for (RequestState state : states) {
          if (input.getState() == state) {
            return true;
          }
        }
        return false;
      }

    });
  }

  private Iterable<SingularityRequestWithState> getRequests(RequestState... states) {
    return filter(getRequests(), states);
  }

  public Iterable<SingularityRequestWithState> getPausedRequests() {
    return getRequests(RequestState.PAUSED);
  }

  public Iterable<SingularityRequestWithState> getActiveRequests() {
    return getRequests(RequestState.ACTIVE, RequestState.DEPLOYING_TO_UNPAUSE);
  }

  public Iterable<SingularityRequestWithState> getCooldownRequests() {
    return getRequests(RequestState.SYSTEM_COOLDOWN);
  }

  public Iterable<SingularityRequestWithState> getFinishedRequests() {
    return getRequests(RequestState.FINISHED);
  }

  public List<SingularityRequestWithState> getRequests() {
    return getAsyncChildren(NORMAL_PATH_ROOT, requestTranscoder);
  }

  public Optional<SingularityRequestWithState> getRequest(String requestId) {
    return getData(getRequestPath(requestId), requestTranscoder);
  }

  public Optional<SingularityRequestCleanup> getCleanupRequest(String requestId) {
    return getData(getCleanupPath(requestId), requestCleanupTranscoder);
  }

  public void deleteRequest(SingularityRequest request, Optional<String> user) {
    createCleanupRequest(new SingularityRequestCleanup(user, RequestCleanupType.DELETING, System.currentTimeMillis(), Optional.of(Boolean.TRUE), request.getId()));

    saveHistory(new SingularityRequestHistory(System.currentTimeMillis(), user, RequestHistoryType.DELETED, request));

    delete(getRequestPath(request.getId()));
  }

}
