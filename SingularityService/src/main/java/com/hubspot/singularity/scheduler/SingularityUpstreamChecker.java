package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.base.Optional;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.singularity.LoadBalancerRequestType;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
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

  private SingularityLoadBalancerUpdate syncUpstreamsForService(SingularityRequest request, String requestId, SingularityDeploy deploy, Optional<String> loadBalancerUpstreamGroup) {
    Collection<UpstreamInfo> upstreamsInBaragonForRequest = lbClient.getBaragonUpstreamsForRequest(requestId);
    Collection<UpstreamInfo> upstreamsInSingularityForRequest = getUpstreamsFromActiveTasks(requestId, loadBalancerUpstreamGroup);
    upstreamsInBaragonForRequest.removeAll(upstreamsInSingularityForRequest);
    LoadBalancerRequestId loadBalancerRequestId = new LoadBalancerRequestId(requestId, LoadBalancerRequestType.REMOVE, Optional.absent());
    return lbClient.makeAndSendBaragonRequest(loadBalancerRequestId, Collections.emptyList(), new ArrayList<>(upstreamsInBaragonForRequest), deploy, request);
  }

  public void syncUpstreams() {
    for (SingularityRequestWithState singularityRequestWithState: requestManager.getActiveRequests()){
      SingularityRequest request = singularityRequestWithState.getRequest();
      if (request.isLoadBalanced()) {
        //TODO: lock on the requestId
        String requestId = singularityRequestWithState.getRequest().getId();
        String deployId = deployManager.getInUseDeployId(requestId).get(); //TODO: handle when it's absent
        SingularityDeploy deploy = deployManager.getDeploy(requestId, deployId).get();
        Optional<String> loadBalancerUpstreamGroup = deploy.getLoadBalancerUpstreamGroup(); // TODO: handle when absent
        syncUpstreamsForService(request, requestId, deploy, loadBalancerUpstreamGroup);
      }
    }
  }
}
