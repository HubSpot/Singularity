package com.hubspot.singularity.api.common;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;

@Immutable
@SingularityStyle
public abstract class AbstractLoadBalancerRequestId {
  public abstract String getId();

  public abstract LoadBalancerRequestType getRequestType();

  @Default
  public Integer getAttemptNumber() {
    return 1;
  }

  @Override
  public String toString() {
    return String.format("%s-%s-%s", getId(), getRequestType(), getAttemptNumber());
  }
}
