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
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.hooks.LoadBalancerClientImpl;

@Singleton
public class SingularityUpstreamChecker {

  private final LoadBalancerClientImpl lbClient;
  private final TaskManager taskManager;
  private final RequestManager requestManager;

  @Inject
  public SingularityUpstreamChecker(LoadBalancerClientImpl lbClient, TaskManager taskManager, RequestManager requestManager) {
    this.lbClient = lbClient;
    this.taskManager = taskManager;
    this.requestManager = requestManager;
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

  private void syncUpstreamsForService(SingularityRequest request, Optional<String> loadBalancerUpstreamGroup) {
    String requestId = request.getId();
    Collection<UpstreamInfo> upstreamsInBaragonForRequest = lbClient.getBaragonUpstreamsForRequest(requestId);
    Collection<UpstreamInfo> upstreamsInSingularityForRequest = getUpstreamsFromActiveTasks(requestId, loadBalancerUpstreamGroup);
    upstreamsInBaragonForRequest.removeAll(upstreamsInSingularityForRequest);
    LoadBalancerRequestId loadBalancerRequestId = new LoadBalancerRequestId(requestId, LoadBalancerRequestType.REMOVE, Optional.absent());
    for (UpstreamInfo upstream: upstreamsInBaragonForRequest){
      //TODO: remove the upstream from Baragon
    }
  }

  public void syncUpstreams() {
    // TODO: check through the active requests and run the method above

  }
}
