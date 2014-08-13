package com.hubspot.singularity.data;

import java.util.Collection;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestCleanup.RequestCleanupType;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.SingularityPendingRequestTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityRequestCleanupTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityRequestWithStateTranscoder;

public class RequestManager extends CuratorAsyncManager {

  private final static Logger LOG = LoggerFactory.getLogger(RequestManager.class);

  private final SingularityRequestWithStateTranscoder requestTranscoder;
  private final SingularityPendingRequestTranscoder pendingRequestTranscoder;
  private final SingularityRequestCleanupTranscoder requestCleanupTranscoder;

  private final static String REQUEST_ROOT = "/requests";

  private final static String NORMAL_PATH_ROOT = REQUEST_ROOT + "/all";
  private final static String PENDING_PATH_ROOT = REQUEST_ROOT + "/pending";
  private final static String CLEANUP_PATH_ROOT = REQUEST_ROOT +  "/cleanup";

  @Inject
  public RequestManager(SingularityConfiguration configuration, CuratorFramework curator, SingularityRequestCleanupTranscoder requestCleanupTranscoder, SingularityRequestWithStateTranscoder requestTranscoder, SingularityPendingRequestTranscoder pendingRequestTranscoder) {
    super(curator, configuration.getZookeeperAsyncTimeout());

    this.requestTranscoder = requestTranscoder;
    this.requestCleanupTranscoder = requestCleanupTranscoder;
    this.pendingRequestTranscoder = pendingRequestTranscoder;
  }

  private String getRequestPath(String requestId) {
    return ZKPaths.makePath(NORMAL_PATH_ROOT, requestId);
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

  public void deleteCleanRequest(String requestId) {
    delete(getCleanupPath(requestId));
  }

  public SingularityCreateResult createCleanupRequest(SingularityRequestCleanup cleanupRequest) {
    return create(getCleanupPath(cleanupRequest.getRequestId()), cleanupRequest, requestCleanupTranscoder);
  }

  private SingularityCreateResult save(SingularityRequest request, RequestState state) {
    return save(getRequestPath(request.getId()), new SingularityRequestWithState(request, state), requestTranscoder);
  }

  public SingularityCreateResult pause(SingularityRequest request) {
    return save(request, RequestState.PAUSED);
  }

  public SingularityCreateResult cooldown(SingularityRequest request) {
    return save(request, RequestState.SYSTEM_COOLDOWN);
  }

  public SingularityCreateResult finish(SingularityRequest request) {
    return save(request, RequestState.FINISHED);
  }

  public SingularityCreateResult addToPendingQueue(SingularityPendingRequest pendingRequest) {
    SingularityCreateResult result = create(getPendingPath(pendingRequest.getRequestId(), pendingRequest.getDeployId()), pendingRequest, pendingRequestTranscoder);

    LOG.info("{} added to pending queue with result: {}", pendingRequest, result);

    return result;
  }

  public SingularityCreateResult makeActive(SingularityRequest request) {
    return saveRequest(request);
  }

  public SingularityCreateResult saveRequest(SingularityRequest request) {
    return save(request, RequestState.ACTIVE);
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

  private Iterable<SingularityRequestWithState> filter(List<SingularityRequestWithState> requests, final RequestState state) {
    return Iterables.filter(requests, new Predicate<SingularityRequestWithState>() {

      @Override
      public boolean apply(SingularityRequestWithState input) {
        return input.getState() == state;
      }

    });
  }

  private Iterable<SingularityRequestWithState> getRequests(RequestState state) {
    return filter(getRequests(), state);
  }

  public Iterable<SingularityRequestWithState> getPausedRequests() {
    return getRequests(RequestState.PAUSED);
  }

  public Iterable<SingularityRequestWithState> getActiveRequests() {
    return getRequests(RequestState.ACTIVE);
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

  public void deleteRequest(Optional<String> user, String requestId) {
    createCleanupRequest(new SingularityRequestCleanup(user, RequestCleanupType.DELETING, System.currentTimeMillis(), Optional.of(Boolean.TRUE), requestId));
    delete(getRequestPath(requestId));
  }

}
