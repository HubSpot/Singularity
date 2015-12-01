package com.hubspot.singularity.scheduler;

import java.util.List;
import java.util.Set;

import com.google.common.base.Optional;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityLoadBalancerUpdate.LoadBalancerMethod;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.hooks.LoadBalancerClient;

public class TestingLoadBalancerClient implements LoadBalancerClient {

  private BaragonRequestState requestState;

  public TestingLoadBalancerClient() {
    requestState = BaragonRequestState.WAITING;
  }

  public void setNextBaragonRequestState(BaragonRequestState nextState) {
    this.requestState = nextState;
  }

  private SingularityLoadBalancerUpdate getReturnValue(LoadBalancerRequestId loadBalancerRequestId, LoadBalancerMethod method) {
    return new SingularityLoadBalancerUpdate(requestState, loadBalancerRequestId, Optional.<String> absent(), System.currentTimeMillis(), method, Optional.<String> absent());
  }

  @Override
  public SingularityLoadBalancerUpdate enqueue(LoadBalancerRequestId loadBalancerRequestId, SingularityRequest request, SingularityDeploy deploy, List<SingularityTask> add, List<SingularityTask> remove) {
    return getReturnValue(loadBalancerRequestId, LoadBalancerMethod.ENQUEUE);
  }

  @Override
  public SingularityLoadBalancerUpdate getState(LoadBalancerRequestId loadBalancerRequestId) {
    return getReturnValue(loadBalancerRequestId, LoadBalancerMethod.CHECK_STATE);
  }

  @Override
  public SingularityLoadBalancerUpdate cancel(LoadBalancerRequestId loadBalancerRequestId) {
    return getReturnValue(loadBalancerRequestId, LoadBalancerMethod.CANCEL);
  }

  @Override
  public SingularityLoadBalancerUpdate delete(LoadBalancerRequestId loadBalancerRequestId, String requestId, Set<String> loadBalancerGroups, String serviceBasePath) {
    return getReturnValue(loadBalancerRequestId, LoadBalancerMethod.DELETE);
  }

}
