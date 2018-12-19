package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.zookeeper.Op;

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

  private List<SingularityTask> getActiveTasksForRequest(String requestId) {
    final Map<SingularityTaskId, SingularityTask> activeTasksForRequest = taskManager.getTasks(taskManager.getActiveTaskIdsForRequest(requestId));
    return new ArrayList<>(activeTasksForRequest.values());
  }

  private Collection<UpstreamInfo> getUpstreamsFromActiveTasksForRequest(String requestId, Optional<String> loadBalancerUpstreamGroup) {
    return lbClient.getUpstreamsForTasks(getActiveTasksForRequest(requestId), requestId, loadBalancerUpstreamGroup);
  }

  private boolean isEqualUpstreamGroupRackId(UpstreamInfo upstream1, UpstreamInfo upstream2){
    return (upstream1.getUpstream().equals(upstream2.getUpstream()))
        && (upstream1.getGroup().equals(upstream2.getGroup()))
        && (upstream1.getRackId().equals(upstream2.getRackId()));
  }

  private Collection<UpstreamInfo> getEqualUpstreams(UpstreamInfo upstream, Collection<UpstreamInfo> upstreams) {
    return upstreams.stream().filter(candidate -> isEqualUpstreamGroupRackId(candidate, upstream)).collect(Collectors.toList());
  }

  // TODO: confirm if there can only be one match, or it's fine to collect a list of matches as is right now
  private List<UpstreamInfo> getExtraUpstreams(Collection<UpstreamInfo> upstreamsInBaragonForRequest, Collection<UpstreamInfo> upstreamsInSingularityForRequest) {
    for (UpstreamInfo upstreamInSingularity : upstreamsInSingularityForRequest) {
      Collection<UpstreamInfo> matches = getEqualUpstreams(upstreamInSingularity, upstreamsInBaragonForRequest);
      upstreamsInBaragonForRequest.removeAll(matches);
    }
    return new ArrayList<>(upstreamsInBaragonForRequest);
  }

  private SingularityLoadBalancerUpdate syncUpstreamsForService(SingularityRequest request, String requestId, SingularityDeploy deploy, Optional<String> loadBalancerUpstreamGroup) {
    Collection<UpstreamInfo> upstreamsInBaragonForRequest = lbClient.getBaragonUpstreamsForRequest(requestId);
    Collection<UpstreamInfo> upstreamsInSingularityForRequest = getUpstreamsFromActiveTasksForRequest(requestId, loadBalancerUpstreamGroup);
    final List<UpstreamInfo> extraUpstreams = getExtraUpstreams(upstreamsInBaragonForRequest, upstreamsInSingularityForRequest);
    final LoadBalancerRequestId loadBalancerRequestId = new LoadBalancerRequestId(requestId, LoadBalancerRequestType.REMOVE, Optional.absent());
    return lbClient.makeAndSendBaragonRequest(loadBalancerRequestId, Collections.emptyList(), extraUpstreams, deploy, request);
  }

  public void syncUpstreams() {
    for (SingularityRequestWithState singularityRequestWithState: requestManager.getActiveRequests()){
      SingularityRequest request = singularityRequestWithState.getRequest();
      if (request.isLoadBalanced()) {
        String requestId = singularityRequestWithState.getRequest().getId(); //TODO: lock on the requestId
        Optional<String> maybeDeployId = deployManager.getInUseDeployId(requestId);
        if (maybeDeployId.isPresent()) {
          String deployId = maybeDeployId.get();
          Optional<SingularityDeploy> maybeDeploy = deployManager.getDeploy(requestId, deployId);
          if (maybeDeploy.isPresent()) {
            SingularityDeploy deploy = maybeDeploy.get();
            Optional<String> loadBalancerUpstreamGroup = deploy.getLoadBalancerUpstreamGroup();
            syncUpstreamsForService(request, requestId, deploy, loadBalancerUpstreamGroup);
          }
        }
      }
    }
  }
}
