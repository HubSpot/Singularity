package com.hubspot.singularity.data;

import java.util.List;
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityPendingRequestId;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestCleanup.RequestCleanupType;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.SingularityRequestTranscoder;
import com.sun.jersey.api.ConflictException;

public class RequestManager extends CuratorAsyncManager {
  
  private final static Logger LOG = LoggerFactory.getLogger(RequestManager.class);
  
  private final ObjectMapper objectMapper;
  private final SingularityRequestTranscoder requestTranscoder;
  
  private final static String REQUEST_ROOT = "/requests";
    
  private final static String ACTIVE_PATH_ROOT = REQUEST_ROOT + "/active";
  private final static String PAUSED_PATH_ROOT = REQUEST_ROOT + "/paused";
  private final static String PENDING_PATH_ROOT = REQUEST_ROOT + "/pending";
  private final static String CLEANUP_PATH_ROOT = REQUEST_ROOT +  "/cleanup";
  
  @Inject
  public RequestManager(SingularityConfiguration configuration, CuratorFramework curator, ObjectMapper objectMapper, SingularityRequestTranscoder requestTranscoder) {
    super(curator, configuration.getZookeeperAsyncTimeout());
  
    this.requestTranscoder = requestTranscoder;
    this.objectMapper = objectMapper;
  }
 
  private String getRequestPath(String requestId) {
    return ZKPaths.makePath(ACTIVE_PATH_ROOT, requestId);
  }
  
  private String getPendingPath(String requestId) {
    return ZKPaths.makePath(PENDING_PATH_ROOT, requestId);
  }
  
  private String getCleanupPath(String requestId) {
    return ZKPaths.makePath(CLEANUP_PATH_ROOT, requestId);
  }
  
  private String getPausedPath(String requestId) {
    return ZKPaths.makePath(PAUSED_PATH_ROOT, requestId);
  }
  
  public int getNumPausedRequests() {
    return getNumChildren(PAUSED_PATH_ROOT);
  }
  
  public int getSizeOfPendingQueue() {
    return getNumChildren(PENDING_PATH_ROOT);
  }
  
  public int getSizeOfCleanupQueue() {
    return getNumChildren(CLEANUP_PATH_ROOT);
  }
  
  public int getNumRequests() {
    return getNumChildren(ACTIVE_PATH_ROOT);
  }
  
  public void deletePendingRequest(String pendingRequestId) {
    delete(getPendingPath(pendingRequestId));
  }
  
  public void deleteCleanRequest(String requestId) {
    delete(getCleanupPath(requestId));
  }
 
  public SingularityCreateResult createCleanupRequest(SingularityRequestCleanup cleanupRequest) {
    return create(getCleanupPath(cleanupRequest.getRequestId()), Optional.of(cleanupRequest.getAsBytes(objectMapper)));
  }
  
  public void pause(SingularityRequest request) {
    create(getPausedPath(request.getId()), Optional.of(request.getAsBytes(objectMapper)));
    deleteRequestObject(request.getId());
  }
  
  public boolean isRequestPaused(String requestId) {
    try {
      return curator.checkExists().forPath(getPausedPath(requestId)) != null;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
  
  public SingularityDeleteResult deletePausedRequest(String requestId) {
    return delete(getPausedPath(requestId));
  }
  
  public Optional<SingularityRequest> unpause(String requestId) throws IllegalStateException {
    Optional<SingularityRequest> paused = fetchPausedRequest(requestId);
    
    if (paused.isPresent()) {
      persistRequest(paused.get());
      
      deletePausedRequest(requestId);
    }
  
    return paused;
  }
  
  public Optional<SingularityRequest> fetchPausedRequest(String requestId) {
    return getRequestFromPath(getPausedPath(requestId));
  }
  
  
  public void addToPendingQueue(SingularityPendingRequestId pendingRequestId) {
    addToPendingQueue(pendingRequestId, Optional.<String> absent());
  }
  
  public void addToPendingQueue(SingularityPendingRequestId pendingRequestId, Optional<String> cmdLineArgs) {
    Optional<byte[]> data = null;
    
    if (cmdLineArgs.isPresent()) {
      data = Optional.of(JavaUtils.toBytes(cmdLineArgs.get()));
    } else {
      data = Optional.absent();
    }
    
    create(getPendingPath(pendingRequestId.toString()), data);
  }
  
  public enum PersistResult {
    CREATED, UPDATED;
  }

  public PersistResult persistRequest(SingularityRequest request) {
    try {
      return persistRequestPrivate(request);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  private PersistResult persistRequestPrivate(SingularityRequest request) throws Exception {
    if (curator.checkExists().forPath(getCleanupPath(request.getId())) != null) {
      throw new ConflictException(String.format("A cleanup request exists for %s", request.getId())); // TODO if this was called internally it would be a problem.
    }
    
    final String requestPath = getRequestPath(request.getId());
    final byte[] bytes = request.getAsBytes(objectMapper);
    
    try {
      curator.create().creatingParentsIfNeeded().forPath(requestPath, bytes);
      return PersistResult.CREATED;
    } catch (NodeExistsException nee) {
      curator.setData().forPath(requestPath, bytes);
      return PersistResult.UPDATED;
    }
  }
  
  public List<String> getRequestIds() {
    return getChildren(ACTIVE_PATH_ROOT);
  }
  
  public Optional<String> getPendingRequestCmdLineArgs(String requestId) {
    return getStringData(getPendingPath(requestId));
  }
  
  public List<SingularityPendingRequestId> getPendingRequestIds() {
    List<String> pendingStrings = getChildren(PENDING_PATH_ROOT);
    List<SingularityPendingRequestId> pendingRequestIds = Lists.newArrayListWithCapacity(pendingStrings.size());
    
    for (String pendingString : pendingStrings) {
      pendingRequestIds.add(SingularityPendingRequestId.fromString(pendingString));
    }
    
    return pendingRequestIds;
  }
  
  public List<String> getCleanupRequestIds() {
    return getChildren(CLEANUP_PATH_ROOT);
  }
   
  public List<SingularityRequestCleanup> getCleanupRequests() {
    return fetchCleanupRequests(getCleanupRequestIds());
  }
  
  public List<SingularityRequestCleanup> fetchCleanupRequests(List<String> cleanupRequestIds) {
    final List<SingularityRequestCleanup> cleanupRequests = Lists.newArrayListWithCapacity(cleanupRequestIds.size());
    
    for (String requestId : cleanupRequestIds) {
      Optional<SingularityRequestCleanup> maybeRequestCleanup = fetchCleanupRequest(requestId);
      
      if (maybeRequestCleanup.isPresent()) {
        cleanupRequests.add(maybeRequestCleanup.get());
      }
    }
    
    return cleanupRequests;
  }
  
  public List<SingularityTaskRequest> getTaskRequests(List<SingularityPendingTask> tasks) {
    final Map<String, SingularityPendingTask> requestIdToPendingTaskId = Maps.newHashMapWithExpectedSize(tasks.size());
    
    for (SingularityPendingTask task : tasks) {
      requestIdToPendingTaskId.put(task.getTaskId().getRequestId(), task);
    }
    
    final List<SingularityRequest> matchingRequests = getAsync(ACTIVE_PATH_ROOT, requestIdToPendingTaskId.keySet(), requestTranscoder);
    
    final List<SingularityTaskRequest> taskRequests = Lists.newArrayListWithCapacity(matchingRequests.size());
    
    for (SingularityRequest request : matchingRequests) {
      SingularityPendingTask task = requestIdToPendingTaskId.get(request.getId());
    
      taskRequests.add(new SingularityTaskRequest(request, task.getTaskId(), task.getMaybeCmdLineArgs()));
    }
    
    return taskRequests;
  }
  
  public List<SingularityRequest> getPausedRequests() {
    return getAsyncChildren(PAUSED_PATH_ROOT, requestTranscoder);
  }
  
  public List<SingularityRequest> getActiveRequests() {
    return getAsyncChildren(ACTIVE_PATH_ROOT, requestTranscoder);
  }
  
  private Optional<SingularityRequest> getRequestFromPath(String path) {
    try {
      SingularityRequest request = requestTranscoder.transcode(curator.getData().forPath(path));
      
      return Optional.of(request);
    } catch (NoNodeException nee) {
      return Optional.absent();
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }
  
  public Optional<SingularityRequest> fetchRequest(String requestId) {
    return getRequestFromPath(getRequestPath(requestId));
  }
  
  public Optional<SingularityRequestCleanup> fetchCleanupRequest(String requestId) {
    try {
      SingularityRequestCleanup cleanupRequest = SingularityRequestCleanup.fromBytes(curator.getData().forPath(getCleanupPath(requestId)), objectMapper);
      return Optional.of(cleanupRequest);
    } catch (NoNodeException nee) {
      return Optional.absent();
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }
  
  private void deleteRequestObject(String requestId) {
    delete(getRequestPath(requestId));
  }
  
  public Optional<SingularityRequest> deleteRequest(Optional<String> user, String requestId) {
    Optional<SingularityRequest> request = fetchRequest(requestId);
    
    if (request.isPresent()) {
      createCleanupRequest(new SingularityRequestCleanup(user, RequestCleanupType.DELETING, System.currentTimeMillis(), requestId));
      deleteRequestObject(requestId);
    }
    
    return request;
  }

}
