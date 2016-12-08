package com.hubspot.singularity.data;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.RequestCleanupType;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.SingularityRequestLbCleanup;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityShellCommand;
import com.hubspot.singularity.api.SingularityExpiringRequestParent;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;
import com.hubspot.singularity.event.SingularityEventListener;
import com.hubspot.singularity.expiring.SingularityExpiringBounce;
import com.hubspot.singularity.expiring.SingularityExpiringParent;
import com.hubspot.singularity.expiring.SingularityExpiringPause;
import com.hubspot.singularity.expiring.SingularityExpiringRequestActionParent;
import com.hubspot.singularity.expiring.SingularityExpiringScale;
import com.hubspot.singularity.expiring.SingularityExpiringSkipHealthchecks;

@Singleton
public class RequestManager extends CuratorAsyncManager {

  private static final Logger LOG = LoggerFactory.getLogger(RequestManager.class);

  private final Transcoder<SingularityRequestWithState> requestTranscoder;
  private final Transcoder<SingularityPendingRequest> pendingRequestTranscoder;
  private final Transcoder<SingularityRequestCleanup> requestCleanupTranscoder;
  private final Transcoder<SingularityRequestHistory> requestHistoryTranscoder;
  private final Transcoder<SingularityRequestLbCleanup> requestLbCleanupTranscoder;

  private final SingularityEventListener singularityEventListener;

  private static final String REQUEST_ROOT = "/requests";

  private static final String NORMAL_PATH_ROOT = REQUEST_ROOT + "/all";
  private static final String PENDING_PATH_ROOT = REQUEST_ROOT + "/pending";
  private static final String CLEANUP_PATH_ROOT = REQUEST_ROOT + "/cleanup";
  private static final String HISTORY_PATH_ROOT = REQUEST_ROOT + "/history";
  private static final String LB_CLEANUP_PATH_ROOT = REQUEST_ROOT + "/lbCleanup";
  private static final String EXPIRING_ACTION_PATH_ROOT = REQUEST_ROOT + "/expiring";
  private static final String EXPIRING_BOUNCE_PATH_ROOT = EXPIRING_ACTION_PATH_ROOT + "/bounce";
  private static final String EXPIRING_PAUSE_PATH_ROOT = EXPIRING_ACTION_PATH_ROOT + "/pause";
  private static final String EXPIRING_SCALE_PATH_ROOT = EXPIRING_ACTION_PATH_ROOT + "/scale";
  private static final String EXPIRING_SKIP_HC_PATH_ROOT = EXPIRING_ACTION_PATH_ROOT + "/skipHc";

  private static final Map<Class<? extends SingularityExpiringRequestActionParent<? extends SingularityExpiringRequestParent>>, String> EXPIRING_CLASS_TO_PATH = ImmutableMap.of(
      SingularityExpiringBounce.class, EXPIRING_BOUNCE_PATH_ROOT,
      SingularityExpiringPause.class, EXPIRING_PAUSE_PATH_ROOT,
      SingularityExpiringScale.class, EXPIRING_SCALE_PATH_ROOT,
      SingularityExpiringSkipHealthchecks.class, EXPIRING_SKIP_HC_PATH_ROOT
      );

  private final Map<Class<? extends SingularityExpiringRequestActionParent<? extends SingularityExpiringRequestParent>>, Transcoder<? extends SingularityExpiringRequestActionParent<? extends SingularityExpiringRequestParent>>> expiringTranscoderMap;

  @Inject
  public RequestManager(CuratorFramework curator, SingularityConfiguration configuration, MetricRegistry metricRegistry, SingularityEventListener singularityEventListener,
      Transcoder<SingularityRequestCleanup> requestCleanupTranscoder, Transcoder<SingularityRequestWithState> requestTranscoder, Transcoder<SingularityRequestLbCleanup> requestLbCleanupTranscoder,
      Transcoder<SingularityPendingRequest> pendingRequestTranscoder, Transcoder<SingularityRequestHistory> requestHistoryTranscoder, Transcoder<SingularityExpiringBounce> expiringBounceTranscoder,
      Transcoder<SingularityExpiringScale> expiringScaleTranscoder,  Transcoder<SingularityExpiringPause> expiringPauseTranscoder, Transcoder<SingularityExpiringSkipHealthchecks> expiringSkipHealthchecksTranscoder) {
    super(curator, configuration, metricRegistry);
    this.requestTranscoder = requestTranscoder;
    this.requestCleanupTranscoder = requestCleanupTranscoder;
    this.pendingRequestTranscoder = pendingRequestTranscoder;
    this.requestHistoryTranscoder = requestHistoryTranscoder;
    this.singularityEventListener = singularityEventListener;
    this.requestLbCleanupTranscoder = requestLbCleanupTranscoder;

    this.expiringTranscoderMap = ImmutableMap.of(
        SingularityExpiringBounce.class, expiringBounceTranscoder,
        SingularityExpiringPause.class, expiringPauseTranscoder,
        SingularityExpiringScale.class, expiringScaleTranscoder,
        SingularityExpiringSkipHealthchecks.class, expiringSkipHealthchecksTranscoder
        );
  }

  private String getRequestPath(String requestId) {
    return ZKPaths.makePath(NORMAL_PATH_ROOT, requestId);
  }

  @SuppressWarnings("unchecked")
  private <T extends SingularityExpiringRequestActionParent<? extends SingularityExpiringRequestParent>> String getExpiringPath(T expiringObject) {
    return getExpiringPath(expiringObject.getClass(), expiringObject.getRequestId());
  }

  private <T extends SingularityExpiringRequestActionParent<? extends SingularityExpiringRequestParent>> String getExpiringPath(Class<T> clazz, String requestId) {
    return ZKPaths.makePath(EXPIRING_CLASS_TO_PATH.get(clazz), requestId);
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

  private String getPendingPath(SingularityPendingRequest pendingRequest) {
    String nodeName = String.format("%s%s",
      new SingularityDeployKey(pendingRequest.getRequestId(), pendingRequest.getDeployId()),
      pendingRequest.getPendingType().equals(PendingType.ONEOFF) ? pendingRequest.getTimestamp()  : "");
    return ZKPaths.makePath(PENDING_PATH_ROOT, nodeName);
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
    return delete(getPendingPath(pendingRequest));
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

  public boolean cleanupRequestExists(String requestId, RequestCleanupType type) {
    return checkExists(getCleanupPath(requestId, type)).isPresent();
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

  public SingularityCreateResult update(SingularityRequest request, long timestamp, Optional<String> user, Optional<String> message) {
    return save(request, getRequest(request.getId()).get().getState(), RequestHistoryType.UPDATED, timestamp, user, message);
  }

  public SingularityCreateResult save(SingularityRequest request, RequestState state, RequestHistoryType eventType, long timestamp, Optional<String> user, Optional<String> message) {
    saveHistory(new SingularityRequestHistory(timestamp, user, eventType, request, message));

    return save(getRequestPath(request.getId()), new SingularityRequestWithState(request, state, timestamp), requestTranscoder);
  }

  public SingularityCreateResult pause(SingularityRequest request, long timestamp, Optional<String> user, Optional<String> message) {
    return save(request, RequestState.PAUSED, RequestHistoryType.PAUSED, timestamp, user, message);
  }

  public SingularityCreateResult cooldown(SingularityRequest request, long timestamp) {
    return save(request, RequestState.SYSTEM_COOLDOWN, RequestHistoryType.ENTERED_COOLDOWN, timestamp, Optional.<String> absent(), Optional.<String> absent());
  }

  public SingularityCreateResult finish(SingularityRequest request, long timestamp) {
    return save(request, RequestState.FINISHED, RequestHistoryType.FINISHED, timestamp, Optional.<String> absent(), Optional.<String> absent());
  }

  public SingularityCreateResult addToPendingQueue(SingularityPendingRequest pendingRequest) {
    SingularityCreateResult result = create(getPendingPath(pendingRequest), pendingRequest, pendingRequestTranscoder);

    LOG.info("{} added to pending queue with result: {}", pendingRequest, result);

    return result;
  }

  public Optional<SingularityPendingRequest> getPendingRequest(String requestId, String deployId) {
    return getData(getPendingPath(requestId, deployId), pendingRequestTranscoder);
  }

  public SingularityCreateResult saveHistory(SingularityRequestHistory history) {
    final String path = getHistoryPath(history);

    singularityEventListener.requestHistoryEvent(history);

    return save(path, history, requestHistoryTranscoder);
  }

  public SingularityCreateResult unpause(SingularityRequest request, long timestamp, Optional<String> user, Optional<String> message) {
    return activate(request, RequestHistoryType.UNPAUSED, timestamp, user, message);
  }

  public SingularityCreateResult exitCooldown(SingularityRequest request, long timestamp, Optional<String> user, Optional<String> message) {
    return activate(request, RequestHistoryType.EXITED_COOLDOWN, timestamp, user, message);
  }

  public SingularityCreateResult bounce(SingularityRequest request, long timestamp, Optional<String> user, Optional<String> message) {
    return activate(request, RequestHistoryType.BOUNCED, timestamp, user, message);
  }

  public SingularityCreateResult deployToUnpause(SingularityRequest request, long timestamp, Optional<String> user, Optional<String> message) {
    return save(request, RequestState.DEPLOYING_TO_UNPAUSE, RequestHistoryType.DEPLOYED_TO_UNPAUSE, timestamp, user, message);
  }

  public SingularityCreateResult activate(SingularityRequest request, RequestHistoryType historyType, long timestamp, Optional<String> user, Optional<String> message) {
    return save(request, RequestState.ACTIVE, historyType, timestamp, user, message);
  }

  public SingularityCreateResult update(SingularityRequest request, RequestState requestState, RequestHistoryType historyType, long timestamp, Optional<String> user, Optional<String> message) {
    return save(request, requestState, historyType, timestamp, user, message);
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

  public SingularityDeleteResult deleteRequest(SingularityRequest request, Optional<String> user, Optional<String> actionId, Optional<String> message) {
    final long now = System.currentTimeMillis();

    // delete it no matter if the delete request already exists.
    createCleanupRequest(new SingularityRequestCleanup(user, RequestCleanupType.DELETING, now, Optional.of(Boolean.TRUE), request.getId(), Optional.<String> absent(),
        Optional.<Boolean> absent(), message, actionId, Optional.<SingularityShellCommand>absent()));

    saveHistory(new SingularityRequestHistory(now, user, RequestHistoryType.DELETED, request, message));

    SingularityDeleteResult deleteResult = delete(getRequestPath(request.getId()));

    LOG.info("Request {} deleted ({}) by {} - {}", request.getId(), deleteResult, user, message);

    return deleteResult;
  }

  public List<SingularityRequestLbCleanup> getLbCleanupRequests() {
    return getAsyncChildren(LB_CLEANUP_PATH_ROOT, requestLbCleanupTranscoder);
  }

  public Optional<SingularityRequestLbCleanup> getLbCleanupRequest(String requestId) {
    return getData(getLbCleanupPath(requestId), requestLbCleanupTranscoder);
  }

  public List<String> getLbCleanupRequestIds() {
    return getChildren(LB_CLEANUP_PATH_ROOT);
  }

  private String getLbCleanupPath(String requestId) {
    return ZKPaths.makePath(LB_CLEANUP_PATH_ROOT, requestId);
  }

  public void saveLbCleanupRequest(SingularityRequestLbCleanup cleanup) {
    save(getLbCleanupPath(cleanup.getRequestId()), cleanup, requestLbCleanupTranscoder);
  }

  public SingularityDeleteResult deleteLbCleanupRequest(String requestId) {
    return delete(getLbCleanupPath(requestId));
  }

  public <T extends SingularityExpiringRequestActionParent<? extends SingularityExpiringRequestParent>> List<T> getExpiringObjects(Class<T> clazz) {
    return getAsyncChildren(EXPIRING_CLASS_TO_PATH.get(clazz), getTranscoder(clazz));
  }

  @SuppressWarnings("unchecked")
  private <T extends SingularityExpiringRequestActionParent<? extends SingularityExpiringRequestParent>> Transcoder<T> getTranscoder(Class<T> clazz) {
    return (Transcoder<T>) expiringTranscoderMap.get(clazz);
  }

  @SuppressWarnings("unchecked")
  private <T extends SingularityExpiringRequestActionParent<? extends SingularityExpiringRequestParent>> Transcoder<T> getTranscoder(T expiringObject) {
    return getTranscoder((Class<T>) expiringObject.getClass());
  }

  public <T extends SingularityExpiringRequestActionParent<? extends SingularityExpiringRequestParent>> Optional<T> getExpiringObject(Class<T> clazz, String requestId) {
    return getData(getExpiringPath(clazz, requestId), getTranscoder(clazz));
  }

  public <T extends SingularityExpiringRequestActionParent<? extends SingularityExpiringRequestParent>> SingularityCreateResult saveExpiringObject(T expiringObject) {
    return save(getExpiringPath(expiringObject), expiringObject, getTranscoder(expiringObject));
  }

  public <T extends SingularityExpiringRequestActionParent<? extends SingularityExpiringRequestParent>> SingularityDeleteResult deleteExpiringObject(Class<T> clazz, String requestId) {
    return delete(getExpiringPath(clazz, requestId));
  }

  public Optional<SingularityExpiringBounce> getExpiringBounce(String requestId) {
    return getExpiringObject(SingularityExpiringBounce.class, requestId);
  }

  public Optional<SingularityExpiringPause> getExpiringPause(String requestId) {
    return getExpiringObject(SingularityExpiringPause.class, requestId);
  }

  public Optional<SingularityExpiringScale> getExpiringScale(String requestId) {
    return getExpiringObject(SingularityExpiringScale.class, requestId);
  }

  public Optional<SingularityExpiringSkipHealthchecks> getExpiringSkipHealthchecks(String requestId) {
    return getExpiringObject(SingularityExpiringSkipHealthchecks.class, requestId);
  }

}
