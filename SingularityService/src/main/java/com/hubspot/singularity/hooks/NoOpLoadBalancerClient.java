package com.hubspot.singularity.hooks;

import com.google.inject.Inject;
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.SingularityCheckingUpstreamsUpdate;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class NoOpLoadBalancerClient implements LoadBalancerClient {

  @Inject
  public NoOpLoadBalancerClient() {}

  @Override
  public boolean isEnabled() {
    return false;
  }

  @Override
  public SingularityLoadBalancerUpdate enqueue(
    LoadBalancerRequestId loadBalancerRequestId,
    SingularityRequest request,
    SingularityDeploy deploy,
    List<SingularityTask> add,
    List<SingularityTask> remove
  ) {
    return null;
  }

  @Override
  public SingularityLoadBalancerUpdate getState(
    LoadBalancerRequestId loadBalancerRequestId
  ) {
    return null;
  }

  @Override
  public SingularityLoadBalancerUpdate cancel(
    LoadBalancerRequestId loadBalancerRequestId
  ) {
    return null;
  }

  @Override
  public SingularityLoadBalancerUpdate delete(
    LoadBalancerRequestId loadBalancerRequestId,
    String requestId,
    Set<String> loadBalancerGroups,
    String serviceBasePath
  ) {
    return null;
  }

  @Override
  public SingularityCheckingUpstreamsUpdate getLoadBalancerServiceStateForRequest(
    String singularityRequestId
  )
    throws IOException, InterruptedException, ExecutionException, TimeoutException {
    return null;
  }

  @Override
  public List<UpstreamInfo> getUpstreamsForTasks(
    List<SingularityTask> tasks,
    String requestId,
    Optional<String> loadBalancerUpstreamGroup
  ) {
    return null;
  }

  @Override
  public SingularityLoadBalancerUpdate makeAndSendLoadBalancerRequest(
    LoadBalancerRequestId loadBalancerRequestId,
    List<UpstreamInfo> addUpstreams,
    List<UpstreamInfo> removeUpstreams,
    SingularityDeploy deploy,
    SingularityRequest request
  ) {
    return null;
  }
}
