package com.hubspot.singularity.hooks;

import java.util.List;

import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;

public interface LoadBalancerClient {

  public SingularityLoadBalancerUpdate enqueue(LoadBalancerRequestId loadBalancerRequestId, SingularityRequest request, SingularityDeploy deploy, List<SingularityTask> add, List<SingularityTask> remove);

  public SingularityLoadBalancerUpdate getState(LoadBalancerRequestId loadBalancerRequestId);

  public SingularityLoadBalancerUpdate cancel(LoadBalancerRequestId loadBalancerRequestId);

}
