package com.hubspot.singularity.api.common;

import java.util.Optional;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "An updated to load balancer configuration")
public abstract class AbstractSingularityLoadBalancerUpdate {
  @JsonProperty("state")
  @Schema(description = "The current state of the request to update load balancer configuration")
  public abstract BaragonRequestState getLoadBalancerState();

  @Schema(description = "A unique id describing this load balancer update")
  public abstract LoadBalancerRequestId getLoadBalancerRequestId();

  @Schema(description = "An optional message accompanying the load balancer update")
  public abstract Optional<String> getMessage();

  @Schema(description = "The time at which this update occured")
  public abstract long getTimestamp();

  @Schema(description = "Describes the reason for this load balancer update")
  public abstract LoadBalancerMethod getMethod();

  @Schema(description = "The uri used to update the load balancer configuration")
  public abstract Optional<String> getUri();


  public static SingularityLoadBalancerUpdate preEnqueue(LoadBalancerRequestId lbRequestId) {
    return new SingularityLoadBalancerUpdate(BaragonRequestState.UNKNOWN, lbRequestId, Optional.empty(), System.currentTimeMillis(), LoadBalancerMethod.PRE_ENQUEUE,
      Optional.empty());
  }
}
