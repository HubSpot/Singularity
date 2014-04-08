package com.hubspot.singularity.data;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployKey;
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
      requestIdToPendingTaskId.put(task.getPendingTaskId().getRequestId(), task);
    }
    
    final List<SingularityRequest> matchingRequests = requestManager.getRequests(requestIdToPendingTaskId.keySet());
    
    final Map<SingularityPendingTask, SingularityDeployKey> deployKeys = SingularityDeployKey.fromPendingTasks(requestIdToPendingTaskId.values());
    final Map<SingularityDeployKey, SingularityDeploy> matchingDeploys = deployManager.getDeploysForKeys(deployKeys.values());
    
    final List<SingularityTaskRequest> taskRequests = Lists.newArrayListWithCapacity(matchingRequests.size());
    
    for (SingularityRequest request : matchingRequests) {
      SingularityPendingTask task = requestIdToPendingTaskId.get(request.getId());
    
      SingularityDeploy foundDeploy = matchingDeploys.get(deployKeys.get(task));
      
      if (foundDeploy == null) {
        LOG.warn(String.format("Couldn't find a matching deploy for pending task %s", task));
        continue;
      }
      
      taskRequests.add(new SingularityTaskRequest(request, foundDeploy, task));
    }
    
    return taskRequests;
  }
  
}
