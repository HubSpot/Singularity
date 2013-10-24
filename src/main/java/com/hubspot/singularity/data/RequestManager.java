package com.hubspot.singularity.data;

import java.util.Collections;
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
import com.hubspot.singularity.SingularityRequest;

public class RequestManager {
  
  private final static Logger LOG = LoggerFactory.getLogger(RequestManager.class);
  
  private final CuratorFramework curator;
  private final ObjectMapper objectMapper;

  private final static String REQUEST_PATH_ROOT = "/requests";
  private final static String REQUEST_PATH_FORMAT = REQUEST_PATH_ROOT + "/%s";
    
  @Inject
  public RequestManager(CuratorFramework curator, ObjectMapper objectMapper) {
    this.curator = curator;
    this.objectMapper = objectMapper;
  }

  private String getRequestPath(String name) {
    return String.format(REQUEST_PATH_FORMAT, name);
  }

  public void persistRequest(SingularityRequest request) {
    try {
      persistRequestPrivate(request);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  private void persistRequestPrivate(SingularityRequest request) throws Exception {
    final String requestPath = getRequestPath(request.getName());

    try {
      curator.create().creatingParentsIfNeeded().forPath(requestPath, request.getRequestData(objectMapper));
    } catch (NodeExistsException nee) {
      curator.setData().forPath(requestPath, request.getRequestData(objectMapper));
    }
  }
  
  public List<String> getRequestNames() {
    try {
      return curator.getChildren().forPath(REQUEST_PATH_ROOT);
    } catch (NoNodeException nne) {
      return Collections.emptyList();
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }
  
  public List<SingularityTask> fetchTasks(List<SingularityTaskId> taskIds) {
    final List<SingularityTask> tasks = Lists.newArrayListWithCapacity(taskIds.size());
    
    for (SingularityTaskId taskId : taskIds) {
      Optional<SingularityRequest> maybeRequest = fetchRequest(taskId.getName());
      
      if (maybeRequest.isPresent()) {
        tasks.add(new SingularityTask(maybeRequest.get(), taskId));
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
