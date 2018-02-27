package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "An updated to load balancer configuration")
public class SingularityLoadBalancerUpdate {

  private final BaragonRequestState loadBalancerState;
  private final Optional<String> message;
  private final long timestamp;
  private final Optional<String> uri;
  private final LoadBalancerMethod method;
  private final LoadBalancerRequestId loadBalancerRequestId;

  @Schema
  public enum LoadBalancerMethod {
    PRE_ENQUEUE, ENQUEUE, CHECK_STATE, CANCEL, DELETE;
  }

  @JsonCreator
  public SingularityLoadBalancerUpdate(@JsonProperty("state") BaragonRequestState loadBalancerState, @JsonProperty("loadBalancerRequestId") LoadBalancerRequestId loadBalancerRequestId,
    @JsonProperty("message") Optional<String> message,
    @JsonProperty("timestamp") long timestamp, @JsonProperty("method") LoadBalancerMethod method, @JsonProperty("uri") Optional<String> uri) {
    this.loadBalancerState = loadBalancerState;
    this.message = message;
    this.timestamp = timestamp;
    this.uri = uri;
    this.method = method;
    this.loadBalancerRequestId = loadBalancerRequestId;
  }

  @Schema(description = "The current state of the request to update load balancer configuration")
  public BaragonRequestState getLoadBalancerState() {
    return loadBalancerState;
  }

  @Schema(description = "An optional message accompanying the load balancer update")
  public Optional<String> getMessage() {
    return message;
  }

  @Schema(description = "The time at which this update occured")
  public long getTimestamp() {
    return timestamp;
  }

  @Schema(description = "The uri used to update the load balancer configuration")
  public Optional<String> getUri() {
    return uri;
  }

  @Schema(description = "Describes the reason for this load balancer update")
  public LoadBalancerMethod getMethod() {
    return method;
  }

  @Schema(description = "A unique id describing this load balancer update")
  public LoadBalancerRequestId getLoadBalancerRequestId() {
    return loadBalancerRequestId;
  }

  public static SingularityLoadBalancerUpdate preEnqueue(LoadBalancerRequestId lbRequestId) {
    return new SingularityLoadBalancerUpdate(BaragonRequestState.UNKNOWN, lbRequestId, Optional.<String>absent(), System.currentTimeMillis(), LoadBalancerMethod.PRE_ENQUEUE,
      Optional.<String>absent());
  }

  @Override
  public String toString() {
    return "SingularityLoadBalancerUpdate{" +
        "loadBalancerState=" + loadBalancerState +
        ", message=" + message +
        ", timestamp=" + timestamp +
        ", uri=" + uri +
        ", method=" + method +
        ", loadBalancerRequestId=" + loadBalancerRequestId +
        '}';
  }
}
