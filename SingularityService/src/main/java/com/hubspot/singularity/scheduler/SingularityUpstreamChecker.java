package com.hubspot.singularity.scheduler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.base.Optional;
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.singularity.LoadBalancerRequestType;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdsByStatus;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.helpers.RequestHelper;
import com.hubspot.singularity.hooks.LoadBalancerClientImpl;

@Singleton
public class SingularityUpstreamChecker {

  private final LoadBalancerClientImpl lbClient;
  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final DeployManager deployManager;
  private final RequestHelper requestHelper;

  @Inject
  public SingularityUpstreamChecker(LoadBalancerClientImpl lbClient,
                                    TaskManager taskManager,
                                    RequestManager requestManager,
                                    DeployManager deployManager,
                                    RequestHelper requestHelper) {
    this.lbClient = lbClient;
    this.taskManager = taskManager;
    this.requestManager = requestManager;
    this.deployManager = deployManager;
    this.requestHelper = requestHelper;
  }

  private List<SingularityTask> getActiveTasksForRequest(String requestId) {
    final Optional<SingularityTaskIdsByStatus> taskIdsByStatusForRequest = requestHelper.getTaskIdsByStatusForRequest(requestId);
    if (taskIdsByStatusForRequest.isPresent()) {
      final List<SingularityTaskId> activeHealthyTaskIdsForRequest = taskIdsByStatusForRequest.get().getHealthy();
      final Map<SingularityTaskId, SingularityTask> activeTasksForRequest = taskManager.getTasks(activeHealthyTaskIdsForRequest);
      return new ArrayList<>(activeTasksForRequest.values());
    }
    return new ArrayList<>();
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
    // We expect that the collection will have a maximum of one match, but we will keep it as a collection just in case
    return upstreams.stream().filter(candidate -> isEqualUpstreamGroupRackId(candidate, upstream)).collect(Collectors.toList());
  }

  private List<UpstreamInfo> getExtraUpstreams(Collection<UpstreamInfo> upstreamsInBaragonForRequest, Collection<UpstreamInfo> upstreamsInSingularityForRequest) {
    for (UpstreamInfo upstreamInSingularity : upstreamsInSingularityForRequest) {
      final Collection<UpstreamInfo> matches = getEqualUpstreams(upstreamInSingularity, upstreamsInBaragonForRequest);
      upstreamsInBaragonForRequest.removeAll(matches);
    }
    return new ArrayList<>(upstreamsInBaragonForRequest);
  }

  private SingularityLoadBalancerUpdate syncUpstreamsForService(SingularityRequest singularityRequest, SingularityDeploy deploy, Optional<String> loadBalancerUpstreamGroup) throws InterruptedException, ExecutionException, TimeoutException, IOException {
    final String singularityRequestId = singularityRequest.getId();
    final LoadBalancerRequestId loadBalancerRequestId = new LoadBalancerRequestId(singularityRequestId, LoadBalancerRequestType.REMOVE, Optional.absent());
    Collection<UpstreamInfo> upstreamsInBaragonForRequest = lbClient.getLoadBalancerUpstreamsForRequest(loadBalancerRequestId.toString());
    Collection<UpstreamInfo> upstreamsInSingularityForRequest = getUpstreamsFromActiveTasksForRequest(singularityRequestId, loadBalancerUpstreamGroup);
    final List<UpstreamInfo> extraUpstreams = getExtraUpstreams(upstreamsInBaragonForRequest, upstreamsInSingularityForRequest);
    return lbClient.makeAndSendLoadBalancerRequest(loadBalancerRequestId, Collections.emptyList(), extraUpstreams, deploy, singularityRequest);
  }

  public void syncUpstreams() throws InterruptedException, ExecutionException, TimeoutException, IOException {
    for (SingularityRequestWithState singularityRequestWithState: requestManager.getActiveRequests()){
      final SingularityRequest singularityRequest= singularityRequestWithState.getRequest();
      if (singularityRequest.isLoadBalanced()) {
        final String singularityRequestId = singularityRequest.getId(); //TODO: lock on the requestId
        final Optional<String> maybeDeployId = deployManager.getInUseDeployId(singularityRequestId);
        if (maybeDeployId.isPresent()) {
          final String deployId = maybeDeployId.get();
          final Optional<SingularityDeploy> maybeDeploy = deployManager.getDeploy(singularityRequestId, deployId);
          if (maybeDeploy.isPresent()) {
            final SingularityDeploy deploy = maybeDeploy.get();
            final Optional<String> loadBalancerUpstreamGroup = deploy.getLoadBalancerUpstreamGroup();
            syncUpstreamsForService(singularityRequest, deploy, loadBalancerUpstreamGroup);
            // TODO: confirm that the request succeeded
          }
        }
      }
    }
  }
}
