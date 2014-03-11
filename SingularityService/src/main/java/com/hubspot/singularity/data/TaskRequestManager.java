package com.hubspot.singularity.data;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTaskRequest;

public class TaskRequestManager {

  private final static Logger LOG = LoggerFactory.getLogger(TaskRequestManager.class);
  
  private final DeployManager deployManager;
  private final RequestManager requestManager;
  
  @Inject
  public TaskRequestManager(DeployManager deployManager, RequestManager requestManager) {
    this.deployManager = deployManager;
    this.requestManager = requestManager;
  }

  public List<SingularityTaskRequest> getTaskRequests(List<SingularityPendingTask> tasks) {
    final Map<String, SingularityPendingTask> requestIdToPendingTaskId = Maps.newHashMapWithExpectedSize(tasks.size());
    
    for (SingularityPendingTask task : tasks) {
      requestIdToPendingTaskId.put(task.getTaskId().getRequestId(), task);
    }
    
    final List<SingularityRequest> matchingRequests = requestManager.getRequests(requestIdToPendingTaskId.keySet());
    final List<SingularityDeploy> matchingDeploys = deployManager.getDeploys(tasks);
    
    final Map<SingularityDeployKey, SingularityDeploy> deployKeyToDeploy = Maps.uniqueIndex(matchingDeploys, new Function<SingularityDeploy, SingularityDeployKey>() {
      @Override
      public SingularityDeployKey apply(SingularityDeploy input) {
        return SingularityDeployKey.fromDepoy(input);
      }  
    });
    
    final List<SingularityTaskRequest> taskRequests = Lists.newArrayListWithCapacity(matchingRequests.size());
    
    for (SingularityRequest request : matchingRequests) {
      SingularityPendingTask task = requestIdToPendingTaskId.get(request.getId());
    
      SingularityDeploy foundDeploy = deployKeyToDeploy.get(new SingularityDeployKey(task.getTaskId().getRequestId(), task.getTaskId().getDeployId()));
      
      if (foundDeploy == null) {
        LOG.warn(String.format(""));
        continue;
      }
      
      taskRequests.add(new SingularityTaskRequest(request, foundDeploy, task));
    }
    
    return taskRequests;
  }
  
  private static class SingularityDeployKey {
    
    private final String requestId;
    private final String deployId;

    public static SingularityDeployKey fromDepoy(SingularityDeploy deploy) {
      return new SingularityDeployKey(deploy.getRequestId(), deploy.getId());
    }
    
    public SingularityDeployKey(String requestId, String deployId) {
      this.requestId = requestId;
      this.deployId = deployId;
    }
    
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((deployId == null) ? 0 : deployId.hashCode());
      result = prime * result + ((requestId == null) ? 0 : requestId.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      SingularityDeployKey other = (SingularityDeployKey) obj;
      if (deployId == null) {
        if (other.deployId != null)
          return false;
      } else if (!deployId.equals(other.deployId))
        return false;
      if (requestId == null) {
        if (other.requestId != null)
          return false;
      } else if (!requestId.equals(other.requestId))
        return false;
      return true;
    }
    
  }
  
  
}
