package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.base.Optional;
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.singularity.LoadBalancerRequestType;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.hooks.LoadBalancerClientImpl;

@Singleton
public class SingularityUpstreamChecker {

  private final LoadBalancerClientImpl lbClient;
  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final DeployManager deployManager;

  @Inject
  public SingularityUpstreamChecker(LoadBalancerClientImpl lbClient, TaskManager taskManager, RequestManager requestManager, DeployManager deployManager) {
    this.lbClient = lbClient;
    this.taskManager = taskManager;
    this.requestManager = requestManager;
    this.deployManager = deployManager;
  }

  private List<SingularityTask> getActiveSingularityTasksForRequest (String requestId) {
    List<SingularityTask> activeSingularityTasksForRequest = new ArrayList<>();
    for (SingularityTaskId taskId: taskManager.getActiveTaskIdsForRequest(requestId)){
      Optional<SingularityTask> maybeTask = taskManager.getTask(taskId);
      if (maybeTask.isPresent()) {
        activeSingularityTasksForRequest.add(maybeTask.get());
      }
    }
    return activeSingularityTasksForRequest;
  }

  private Collection<UpstreamInfo> getUpstreamsFromActiveTasks(String requestId, Optional<String> loadBalancerUpstreamGroup) {
    return lbClient.tasksToUpstreams(getActiveSingularityTasksForRequest(requestId), requestId, loadBalancerUpstreamGroup);
  }

  private void syncUpstreamsForService(String requestId, Optional<String> loadBalancerUpstreamGroup) {
    Collection<UpstreamInfo> upstreamsInBaragonForRequest = lbClient.getBaragonUpstreamsForRequest(requestId);
    Collection<UpstreamInfo> upstreamsInSingularityForRequest = getUpstreamsFromActiveTasks(requestId, loadBalancerUpstreamGroup);
    upstreamsInSingularityForRequest.removeAll(upstreamsInBaragonForRequest);
    LoadBalancerRequestId loadBalancerRequestId = new LoadBalancerRequestId(requestId, LoadBalancerRequestType.REMOVE, Optional.absent());
    for (UpstreamInfo upstream: upstreamsInBaragonForRequest){
      //TODO: remove the upstream from Baragon

    }
  }

  public void syncUpstreams() {
    for (SingularityRequestWithState singularityRequestWithState: requestManager.getActiveRequests()){
      SingularityRequest request = singularityRequestWithState.getRequest();
      if (request.isLoadBalanced()) {
        //TODO: lock on the requestId
        String requestId = singularityRequestWithState.getRequest().getId();
        String deployId = deployManager.getInUseDeployId(requestId).get(); //TODO: handle when it's absent
        Optional<String> loadBalancerUpstreamGroup = deployManager.getDeploy(requestId, deployId).get().getLoadBalancerUpstreamGroup(); // TODO: handle when absent
        syncUpstreamsForService(requestId,loadBalancerUpstreamGroup);
      }
    }
  }
}
