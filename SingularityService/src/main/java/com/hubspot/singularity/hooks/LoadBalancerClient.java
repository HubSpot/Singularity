package com.hubspot.singularity.hooks;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.base.Optional;
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;

public interface LoadBalancerClient {

  SingularityLoadBalancerUpdate enqueue(LoadBalancerRequestId loadBalancerRequestId, SingularityRequest request, SingularityDeploy deploy, List<SingularityTask> add, List<SingularityTask> remove);

  SingularityLoadBalancerUpdate getState(LoadBalancerRequestId loadBalancerRequestId);

  SingularityLoadBalancerUpdate cancel(LoadBalancerRequestId loadBalancerRequestId);

  SingularityLoadBalancerUpdate delete(LoadBalancerRequestId loadBalancerRequestId, String requestId, Set<String> loadBalancerGroups, String serviceBasePath);

  //TODO: complete the javadocs below
  /**
   * @param requestId
   * @return
   */
  Collection<UpstreamInfo> getBaragonUpstreamsForRequest (String requestId);

  /**
   * @param tasks
   * @param requestId
   * @param loadBalancerUpstreamGroup
   * @return
   */
  List<UpstreamInfo> tasksToUpstreams(List<SingularityTask> tasks, String requestId, Optional<String> loadBalancerUpstreamGroup);

  /**
   * @param loadBalancerRequestId
   * @param addUpstreams
   * @param removeUpstreams
   * @param deploy
   * @param request
   * @return
   */
  SingularityLoadBalancerUpdate makeAndSendBaragonRequest(LoadBalancerRequestId loadBalancerRequestId, List<UpstreamInfo> addUpstreams, List<UpstreamInfo> removeUpstreams, SingularityDeploy deploy, SingularityRequest request);
}
