package com.hubspot.singularity.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubspot.singularity.LoadBalancerRequestState;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.LoadBalancerUpstream;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityLoadBalancerUpdate.LoadBalancerMethod;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.helpers.MesosProtosUtils;
import com.hubspot.singularity.hooks.LoadBalancerClient;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class TestingLoadBalancerClient extends LoadBalancerClient {
  private LoadBalancerRequestState requestState;

  public TestingLoadBalancerClient(
    SingularityConfiguration configuration,
    ObjectMapper objectMapper
  ) {
    super(configuration, new MesosProtosUtils(objectMapper));
    requestState = LoadBalancerRequestState.WAITING;
  }

  public void setNextRequestState(LoadBalancerRequestState nextState) {
    this.requestState = nextState;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  private SingularityLoadBalancerUpdate getReturnValue(
    LoadBalancerRequestId loadBalancerRequestId,
    LoadBalancerMethod method
  ) {
    return new SingularityLoadBalancerUpdate(
      requestState,
      loadBalancerRequestId,
      Optional.empty(),
      System.currentTimeMillis(),
      method,
      Optional.empty()
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
  public List<LoadBalancerUpstream> getUpstreamsForRequest(String singularityRequestId)
    throws IOException, InterruptedException, ExecutionException, TimeoutException {
    return Collections.emptyList();
  }

  @Override
  public List<LoadBalancerUpstream> getUpstreamsForTasks(
    List<SingularityTask> tasks,
    String requestId,
    Optional<String> loadBalancerUpstreamGroup
  ) {
    return Collections.emptyList();
  }

  @Override
  public SingularityLoadBalancerUpdate makeAndSendLoadBalancerRequest(
    LoadBalancerRequestId loadBalancerRequestId,
    List<LoadBalancerUpstream> addUpstreams,
    List<LoadBalancerUpstream> removeUpstreams,
    SingularityDeploy deploy,
    SingularityRequest request
  ) {
    return getReturnValue(loadBalancerRequestId, LoadBalancerMethod.CHECK_STATE);
  }
}
