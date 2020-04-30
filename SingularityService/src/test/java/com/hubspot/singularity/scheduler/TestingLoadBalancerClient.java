package com.hubspot.singularity.scheduler;

import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.SingularityCheckingUpstreamsUpdate;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityLoadBalancerUpdate.LoadBalancerMethod;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.hooks.LoadBalancerClient;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class TestingLoadBalancerClient implements LoadBalancerClient {
  private BaragonRequestState requestState;

  public TestingLoadBalancerClient() {
    requestState = BaragonRequestState.WAITING;
  }

  public void setNextBaragonRequestState(BaragonRequestState nextState) {
    this.requestState = nextState;
  }

  private SingularityLoadBalancerUpdate getReturnValue(
    LoadBalancerRequestId loadBalancerRequestId,
    LoadBalancerMethod method
  ) {
    return new SingularityLoadBalancerUpdate(
      requestState,
      loadBalancerRequestId,
      Optional.<String>empty(),
      System.currentTimeMillis(),
      method,
      Optional.<String>empty()
    );
  }

  @Override
  public SingularityLoadBalancerUpdate enqueue(
    LoadBalancerRequestId loadBalancerRequestId,
    SingularityRequest request,
    SingularityDeploy deploy,
    List<SingularityTask> add,
    List<SingularityTask> remove
  ) {
    return getReturnValue(loadBalancerRequestId, LoadBalancerMethod.ENQUEUE);
  }

  @Override
  public SingularityLoadBalancerUpdate getState(
    LoadBalancerRequestId loadBalancerRequestId
  ) {
    return getReturnValue(loadBalancerRequestId, LoadBalancerMethod.CHECK_STATE);
  }

  @Override
  public SingularityLoadBalancerUpdate cancel(
    LoadBalancerRequestId loadBalancerRequestId
  ) {
    return getReturnValue(loadBalancerRequestId, LoadBalancerMethod.CANCEL);
  }

  @Override
  public SingularityLoadBalancerUpdate delete(
    LoadBalancerRequestId loadBalancerRequestId,
    String requestId,
    Set<String> loadBalancerGroups,
    String serviceBasePath
  ) {
    return getReturnValue(loadBalancerRequestId, LoadBalancerMethod.DELETE);
  }

  @Override
  public SingularityCheckingUpstreamsUpdate getLoadBalancerServiceStateForRequest(
    String singularityRequestId
  )
    throws IOException, InterruptedException, ExecutionException, TimeoutException {
    return new SingularityCheckingUpstreamsUpdate(Optional.empty(), singularityRequestId);
  }

  @Override
  public List<UpstreamInfo> getUpstreamsForTasks(
    List<SingularityTask> tasks,
    String requestId,
    Optional<String> loadBalancerUpstreamGroup
  ) {
    return Collections.emptyList();
  }

  @Override
  public SingularityLoadBalancerUpdate makeAndSendLoadBalancerRequest(
    LoadBalancerRequestId loadBalancerRequestId,
    List<UpstreamInfo> addUpstreams,
    List<UpstreamInfo> removeUpstreams,
    SingularityDeploy deploy,
    SingularityRequest request
  ) {
    return getReturnValue(loadBalancerRequestId, LoadBalancerMethod.CHECK_STATE);
  }
}
