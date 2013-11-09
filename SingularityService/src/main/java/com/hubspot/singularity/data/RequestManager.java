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
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTaskRequest;

public class RequestManager extends CuratorManager {
  
  private final static Logger LOG = LoggerFactory.getLogger(RequestManager.class);
  
  private final ObjectMapper objectMapper;

  private final static String REQUEST_PATH_ROOT = "/requests";
  private final static String REQUEST_PATH_FORMAT = REQUEST_PATH_ROOT + "/%s";

  private final static String PENDING_PATH_ROOT = "/pending";
  private final static String PENDING_PATH_FORMAT = PENDING_PATH_ROOT + "/%s";
  
  private final static String CLEANUP_PATH_ROOT = "/cleanup";
  private final static String CLEANUP_PATH_FORMAT = CLEANUP_PATH_ROOT + "/%s";
  
  @Inject
  public RequestManager(CuratorFramework curator, ObjectMapper objectMapper) {
    super(curator);
    this.objectMapper = objectMapper;
  }
 
  private String getRequestPath(String name) {
    return String.format(REQUEST_PATH_FORMAT, name);
  }
  
  private String getPendingPath(String name) {
    return String.format(PENDING_PATH_FORMAT, name);
  }
  
  private String getCleanupPath(String name) {
    return String.format(CLEANUP_PATH_FORMAT, name);
  }
  
  public int getSizeOfPendingQueue() {
    return getNumChildren(PENDING_PATH_ROOT);
  }
  
  public int getSizeOfCleanupQueue() {
    return getNumChildren(CLEANUP_PATH_ROOT);
  }
  
  public int getNumRequests() {
    return getNumChildren(REQUEST_PATH_ROOT);
  }
  
  public void deletePendingRequest(String requestName) {
    delete(getPendingPath(requestName));
  }
  
  public void deleteCleanRequest(String requestName) {
    delete(getCleanupPath(requestName));
  }
 
  public void addToCleanupQueue(String requestName) {
    create(getCleanupPath(requestName));
  }
  
  public void addToPendingQueue(String requestName) {
    create(getPendingPath(requestName));
  }

  public void persistRequest(SingularityRequest request) {
    try {
      persistRequestPrivate(request);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  private void persistRequestPrivate(SingularityRequest request) throws Exception {
    Preconditions.checkState(curator.checkExists().forPath(getCleanupPath(request.getName())) == null, "A cleanup request exists for %s", request.getName());
    
    final String requestPath = getRequestPath(request.getName());

    try {
      curator.create().creatingParentsIfNeeded().forPath(requestPath, request.getRequestData(objectMapper));
    } catch (NodeExistsException nee) {
      curator.setData().forPath(requestPath, request.getRequestData(objectMapper));
    }
  }
  
  public List<String> getRequestNames() {
    return getChildren(REQUEST_PATH_ROOT);
  }
  
  public List<String> getPendingRequestNames() {
    return getChildren(PENDING_PATH_ROOT);
  }
  
  public List<String> getCleanupRequestNames() {
    return getChildren(CLEANUP_PATH_ROOT);
  }
  
  public List<SingularityTaskRequest> fetchTasks(List<SingularityPendingTaskId> taskIds) {
    final List<SingularityTaskRequest> tasks = Lists.newArrayListWithCapacity(taskIds.size());
    
    for (SingularityPendingTaskId taskId : taskIds) {
      Optional<SingularityRequest> maybeRequest = fetchRequest(taskId.getName());
      
      if (maybeRequest.isPresent()) {
        tasks.add(new SingularityTaskRequest(maybeRequest.get(), taskId));
      }
    }
    
    return tasks;
  }
  
  public List<SingularityRequest> getKnownRequests() {
    final List<String> requestNames = getRequestNames();
    final List<SingularityRequest> requests = Lists.newArrayListWithCapacity(requestNames.size());
    
    for (String requestName : requestNames) {
      Optional<SingularityRequest> request = fetchRequest(requestName);
      
      if (request.isPresent()) {
        requests.add(request.get());
      } else {
        LOG.warn(String.format("While fetching requests, expected to find request %s but it was not found", requestName));
      }
    }
    
    return requests;
  }

  public Optional<SingularityRequest> fetchRequest(String requestName) {
    try {
      SingularityRequest request = SingularityRequest.getRequestFromData(curator.getData().forPath(ZKPaths.makePath(REQUEST_PATH_ROOT, requestName)), objectMapper);
      return Optional.of(request);
    } catch (NoNodeException nee) {
      return Optional.absent();
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }
  
  public Optional<SingularityRequest> deleteRequest(String requestName) {
    Optional<SingularityRequest> request = fetchRequest(requestName);
    
    if (request.isPresent()) {
      try {
        addToCleanupQueue(requestName);
        
        curator.delete().forPath(getRequestPath(requestName));
      } catch (NoNodeException nee) {
        LOG.warn(String.format("Couldn't find request at %s to delete", requestName));
      } catch (Throwable t) {
        throw Throwables.propagate(t);
      }
    }
    
    return request;
  }
  
}
