package com.hubspot.singularity;

import com.google.common.base.Optional;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;

public class SingularityCheckingUpstreamsUpdate {
  private final BaragonRequestState baragonRequestState;
  private final Optional<BaragonServiceState> baragonServiceState;
  private final LoadBalancerRequestId loadBalancerRequestId;

  public SingularityCheckingUpstreamsUpdate (BaragonRequestState baragonRequestState,Optional<BaragonServiceState> baragonServiceState, LoadBalancerRequestId loadBalancerRequestId) {
    this.baragonRequestState = baragonRequestState;
    this.baragonServiceState = baragonServiceState;
    this.loadBalancerRequestId = loadBalancerRequestId;
  }

  public BaragonRequestState getBaragonRequestState() {
    return baragonRequestState;
  }

  public Optional<BaragonServiceState> getBaragonServiceState() {
    return baragonServiceState;
  }

  public LoadBalancerRequestId getLoadBalancerRequestId() {
    return loadBalancerRequestId;
  }

  @Override
  public String toString() {
    return "SingularityCheckingUpstreamsUpdate{" +
        "baragonRequestState=" + baragonRequestState +
        ", baragonServiceState=" + baragonServiceState +
        ", loadBalancerRequestId=" + loadBalancerRequestId +
        '}';
  }
}
