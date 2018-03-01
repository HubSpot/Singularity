package com.hubspot.singularity.scheduler;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.singularity.api.common.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.api.common.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.api.common.SingularityLoadBalancerUpdate.LoadBalancerMethod;
import com.hubspot.singularity.api.deploy.SingularityDeploy;
import com.hubspot.singularity.api.request.SingularityRequest;
import com.hubspot.singularity.api.task.SingularityTask;
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
    return new SingularityLoadBalancerUpdate(requestState, loadBalancerRequestId, Optional.empty(), System.currentTimeMillis(), method, Optional.empty());
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
