package com.hubspot.singularity.hooks;

import java.util.List;
import java.util.Set;

import com.hubspot.singularity.api.common.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.api.common.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.api.deploy.SingularityDeploy;
import com.hubspot.singularity.api.request.SingularityRequest;
import com.hubspot.singularity.api.task.SingularityTask;

public interface LoadBalancerClient {

  SingularityLoadBalancerUpdate enqueue(LoadBalancerRequestId loadBalancerRequestId, SingularityRequest request, SingularityDeploy deploy, List<SingularityTask> add, List<SingularityTask> remove);

  SingularityLoadBalancerUpdate getState(LoadBalancerRequestId loadBalancerRequestId);

  SingularityLoadBalancerUpdate cancel(LoadBalancerRequestId loadBalancerRequestId);

  SingularityLoadBalancerUpdate delete(LoadBalancerRequestId loadBalancerRequestId, String requestId, Set<String> loadBalancerGroups, String serviceBasePath);
}
