package com.hubspot.singularity.data;

import java.util.List;

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
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityPendingRequestId;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestCleanup.RequestCleanupType;
import com.hubspot.singularity.SingularityTaskRequest;
import com.sun.jersey.api.ConflictException;

public class RequestManager extends CuratorManager {
  
  private final static Logger LOG = LoggerFactory.getLogger(RequestManager.class);
  
  private final ObjectMapper objectMapper;

  private final static String REQUEST_ROOT = "/requests";
    
  private final static String ACTIVE_PATH_ROOT = REQUEST_ROOT + "/active";

  private final static String PAUSED_PATH_ROOT = REQUEST_ROOT + "/paused";

  private final static String PENDING_PATH_ROOT = REQUEST_ROOT + "/pending";
  
  private final static String CLEANUP_PATH_ROOT = REQUEST_ROOT +  "/cleanup";
  
  @Inject
  public RequestManager(CuratorFramework curator, ObjectMapper objectMapper) {
    super(curator);
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
 
  public CreateResult createCleanupRequest(SingularityRequestCleanup cleanupRequest) {
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
  
  public void deletePausedRequest(String requestId) {
    delete(getPausedPath(requestId));
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
    create(getPendingPath(pendingRequestId.toString()));
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
  
  public List<SingularityTaskRequest> fetchTasks(List<SingularityPendingTaskId> taskIds) {
    final List<SingularityTaskRequest> tasks = Lists.newArrayListWithCapacity(taskIds.size());
    
    for (SingularityPendingTaskId taskId : taskIds) {
      Optional<SingularityRequest> maybeRequest = fetchRequest(taskId.getRequestId());
      
      if (maybeRequest.isPresent()) {
        tasks.add(new SingularityTaskRequest(maybeRequest.get(), taskId));
      }
    }
    
    return tasks;
  }
  
  public List<SingularityRequest> getPausedRequests() {
    return getRequests(PAUSED_PATH_ROOT);
  }
  
  public List<SingularityRequest> getActiveRequests() {
    return getRequests(ACTIVE_PATH_ROOT);
  }
  
  private List<SingularityRequest> getRequests(String parent) {
    final List<String> requestIds = getChildren(parent);
    final List<SingularityRequest> requests = Lists.newArrayListWithCapacity(requestIds.size());
    
    for (String requestId : requestIds) {
      Optional<SingularityRequest> request = getRequestFromPath(ZKPaths.makePath(parent, requestId));
      
      if (request.isPresent()) {
        requests.add(request.get());
      } else {
        LOG.warn(String.format("While fetching %s requests, expected to find request %s but it was not found", parent, requestId));
      }
    }
    
    return requests;
  }  
    
  private Optional<SingularityRequest> getRequestFromPath(String path) {
    try {
      SingularityRequest request = SingularityRequest.fromBytes(curator.getData().forPath(path), objectMapper);
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
