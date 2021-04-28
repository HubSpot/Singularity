package com.hubspot.singularity.hooks;

import com.google.inject.Inject;
import com.hubspot.singularity.LoadBalancerRequestState;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.LoadBalancerUpstream;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityLoadBalancerUpdate.LoadBalancerMethod;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.helpers.MesosProtosUtils;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class NoOpLoadBalancerClient extends LoadBalancerClient {

  @Inject
  public NoOpLoadBalancerClient(
    SingularityConfiguration configuration,
    MesosProtosUtils mesosProtosUtils
  ) {
    super(configuration, mesosProtosUtils);
  }

  @Override
  public boolean isEnabled() {
    return false;
  }

  @Override
  public SingularityLoadBalancerUpdate getState(
    LoadBalancerRequestId loadBalancerRequestId
  ) {
    return new SingularityLoadBalancerUpdate(
      LoadBalancerRequestState.SUCCESS,
      loadBalancerRequestId,
      Optional.empty(),
      System.currentTimeMillis(),
      LoadBalancerMethod.CHECK_STATE,
      Optional.empty()
    );
  }

  @Override
  public SingularityLoadBalancerUpdate cancel(
    LoadBalancerRequestId loadBalancerRequestId
  ) {
    return new SingularityLoadBalancerUpdate(
      LoadBalancerRequestState.SUCCESS,
      loadBalancerRequestId,
      Optional.empty(),
      System.currentTimeMillis(),
      LoadBalancerMethod.CANCEL,
      Optional.empty()
    );
  }

  @Override
  public SingularityLoadBalancerUpdate delete(
    LoadBalancerRequestId loadBalancerRequestId,
    String requestId,
    Set<String> loadBalancerGroups,
    String serviceBasePath
  ) {
    return new SingularityLoadBalancerUpdate(
      LoadBalancerRequestState.SUCCESS,
      loadBalancerRequestId,
      Optional.empty(),
      System.currentTimeMillis(),
      LoadBalancerMethod.DELETE,
      Optional.empty()
    );
  }

  @Override
  public List<LoadBalancerUpstream> getUpstreamsForRequest(String singularityRequestId) {
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
    return new SingularityLoadBalancerUpdate(
      LoadBalancerRequestState.SUCCESS,
      loadBalancerRequestId,
      Optional.empty(),
      System.currentTimeMillis(),
      LoadBalancerMethod.CHECK_STATE,
      Optional.empty()
    );
  }
}
