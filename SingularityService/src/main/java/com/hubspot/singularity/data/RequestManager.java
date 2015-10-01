package com.hubspot.singularity.data;

import java.util.Collection;
import java.util.List;

import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestCleanup.RequestCleanupType;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;
import com.hubspot.singularity.event.SingularityEventListener;

@Singleton
public class RequestManager extends CuratorAsyncManager {

  private static final Logger LOG = LoggerFactory.getLogger(RequestManager.class);

  private final Transcoder<SingularityRequestWithState> requestTranscoder;
  private final Transcoder<SingularityPendingRequest> pendingRequestTranscoder;
  private final Transcoder<SingularityRequestCleanup> requestCleanupTranscoder;
  private final Transcoder<SingularityRequestHistory> requestHistoryTranscoder;
  private final Transcoder<SingularityLoadBalancerUpdate> loadBalancerUpdateTranscoder;

  private final SingularityEventListener singularityEventListener;

  private static final String REQUEST_ROOT = "/requests";

  private static final String NORMAL_PATH_ROOT = REQUEST_ROOT + "/all";
  private static final String PENDING_PATH_ROOT = REQUEST_ROOT + "/pending";
  private static final String CLEANUP_PATH_ROOT = REQUEST_ROOT + "/cleanup";
  private static final String HISTORY_PATH_ROOT = REQUEST_ROOT + "/history";
  private static final String LB_CLEANUP_PATH_ROOT = REQUEST_ROOT + "/lbCleanup";

  @Inject
  public RequestManager(CuratorFramework curator, SingularityConfiguration configuration, MetricRegistry metricRegistry, SingularityEventListener singularityEventListener,
      Transcoder<SingularityRequestCleanup> requestCleanupTranscoder, Transcoder<SingularityRequestWithState> requestTranscoder,
      Transcoder<SingularityPendingRequest> pendingRequestTranscoder, Transcoder<SingularityRequestHistory> requestHistoryTranscoder, Transcoder<SingularityLoadBalancerUpdate> loadBalancerHistoryUpdateTranscoder) {
    super(curator, configuration, metricRegistry);

    this.requestTranscoder = requestTranscoder;
    this.requestCleanupTranscoder = requestCleanupTranscoder;
    this.pendingRequestTranscoder = pendingRequestTranscoder;
    this.requestHistoryTranscoder = requestHistoryTranscoder;
    this.singularityEventListener = singularityEventListener;
    this.loadBalancerUpdateTranscoder = loadBalancerHistoryUpdateTranscoder;
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

  private String getCleanupPath(String requestId, RequestCleanupType type) {
    return ZKPaths.makePath(CLEANUP_PATH_ROOT, requestId + "-" + type.name());
  }

  public int getSizeOfPendingQueue() {
    return getNumChildren(PENDING_PATH_ROOT);
  }

  public int getSizeOfCleanupQueue() {
    return getNumChildren(CLEANUP_PATH_ROOT);
  }

  public int getNumLbCleanupRequests() {
    return getNumChildren(LB_CLEANUP_PATH_ROOT);
  }

  public int getNumRequests() {
    return getNumChildren(NORMAL_PATH_ROOT);
  }

  public SingularityDeleteResult deletePendingRequest(SingularityPendingRequest pendingRequest) {
    return delete(getPendingPath(pendingRequest.getRequestId(), pendingRequest.getDeployId()));
  }

  public SingularityDeleteResult deleteHistoryParent(String requestId) {
    return delete(getHistoryParentPath(requestId));
  }

  public SingularityDeleteResult deleteHistoryItem(SingularityRequestHistory history) {
    return delete(getHistoryPath(history));
  }

  public boolean cleanupRequestExists(String requestId) {
    for (RequestCleanupType type : RequestCleanupType.values()) {
      if (checkExists(getCleanupPath(requestId, type)).isPresent()) {
        return true;
      }
      if (Thread.currentThread().isInterrupted()) {
        break;
      }
    }

    return false;
  }

  public void deleteCleanRequest(String requestId, RequestCleanupType type) {
    delete(getCleanupPath(requestId, type));
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
    return create(getCleanupPath(cleanupRequest.getRequestId(), cleanupRequest.getCleanupType()), cleanupRequest, requestCleanupTranscoder);
  }

  public SingularityCreateResult update(SingularityRequest request, long timestamp, Optional<String> user) {
    return save(request, getRequest(request.getId()).get().getState(), RequestHistoryType.UPDATED, timestamp, user);
  }

  private SingularityCreateResult save(SingularityRequest request, RequestState state, RequestHistoryType eventType, long timestamp, Optional<String> user) {
    saveHistory(new SingularityRequestHistory(timestamp, user, eventType, request));

    return save(getRequestPath(request.getId()), new SingularityRequestWithState(request, state, timestamp), requestTranscoder);
  }

  public SingularityCreateResult pause(SingularityRequest request, long timestamp, Optional<String> user) {
    return save(request, RequestState.PAUSED, RequestHistoryType.PAUSED, timestamp, user);
  }

  public SingularityCreateResult cooldown(SingularityRequest request, long timestamp) {
    return save(request, RequestState.SYSTEM_COOLDOWN, RequestHistoryType.ENTERED_COOLDOWN, timestamp, Optional.<String> absent());
  }

  public SingularityCreateResult finish(SingularityRequest request, long timestamp) {
    return save(request, RequestState.FINISHED, RequestHistoryType.FINISHED, timestamp, Optional.<String> absent());
  }

  public SingularityCreateResult addToPendingQueue(SingularityPendingRequest pendingRequest) {
    SingularityCreateResult result = create(getPendingPath(pendingRequest.getRequestId(), pendingRequest.getDeployId()), pendingRequest, pendingRequestTranscoder);

    LOG.info("{} added to pending queue with result: {}", pendingRequest, result);

    return result;
  }

  @VisibleForTesting
  protected SingularityCreateResult saveHistory(SingularityRequestHistory history) {
    final String path = getHistoryPath(history);

    singularityEventListener.requestHistoryEvent(history);

    return save(path, history, requestHistoryTranscoder);
  }

  public SingularityCreateResult unpause(SingularityRequest request, long timestamp, Optional<String> user) {
    return activate(request, RequestHistoryType.UNPAUSED, timestamp, user);
  }

  public SingularityCreateResult exitCooldown(SingularityRequest request, long timestamp, Optional<String> user) {
    return activate(request, RequestHistoryType.EXITED_COOLDOWN, timestamp, user);
  }

  public SingularityCreateResult bounce(SingularityRequest request, long timestamp, Optional<String> user) {
    return activate(request, RequestHistoryType.BOUNCED, timestamp, user);
  }

  public SingularityCreateResult deployToUnpause(SingularityRequest request, long timestamp, Optional<String> user) {
    return save(request, RequestState.DEPLOYING_TO_UNPAUSE, RequestHistoryType.DEPLOYED_TO_UNPAUSE, timestamp, user);
  }

  public SingularityCreateResult activate(SingularityRequest request, RequestHistoryType historyType, long timestamp, Optional<String> user) {
    return save(request, RequestState.ACTIVE, historyType, timestamp, user);
  }

  public List<SingularityPendingRequest> getPendingRequests() {
    return getAsyncChildren(PENDING_PATH_ROOT, pendingRequestTranscoder);
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

  public void deleteRequest(SingularityRequest request, Optional<String> user) {
    final long now = System.currentTimeMillis();

    // delete it no matter if the delete request already exists.
    createCleanupRequest(new SingularityRequestCleanup(user, RequestCleanupType.DELETING, now, Optional.of(Boolean.TRUE), request.getId(), Optional.<String> absent()));

    saveHistory(new SingularityRequestHistory(now, user, RequestHistoryType.DELETED, request));

    delete(getRequestPath(request.getId()));
  }

  public List<String> getLBCleanupRequestIds() {
    return getChildren(LB_CLEANUP_PATH_ROOT);
  }

  private String getLBCleanupPath(String requestId) {
    return ZKPaths.makePath(LB_CLEANUP_PATH_ROOT, requestId);
  }

  public SingularityCreateResult createLBCleanupRequest(String requestId) {
    return create(getLBCleanupPath(requestId));
  }

  public SingularityDeleteResult deleteLBCleanupRequest(String requestId) {
    return delete(getLBCleanupPath(requestId));
  }

  public Optional<SingularityLoadBalancerUpdate> getLoadBalancerState(String requestId) {
    return getData(getLBCleanupPath(requestId), loadBalancerUpdateTranscoder);
  }

  public void saveLoadBalancerState(String requestId, SingularityLoadBalancerUpdate lbUpdate) {
    save(getLBCleanupPath(requestId), lbUpdate, loadBalancerUpdateTranscoder);
  }
}
