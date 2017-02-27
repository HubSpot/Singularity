package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;

public class SingularityLoadBalancerUpdate {

  private final BaragonRequestState loadBalancerState;
  private final Optional<String> message;
  private final long timestamp;
  private final Optional<String> uri;
  private final LoadBalancerMethod method;
  private final LoadBalancerRequestId loadBalancerRequestId;


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

  public BaragonRequestState getLoadBalancerState() {
    return loadBalancerState;
  }

  public Optional<String> getMessage() {
    return message;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public Optional<String> getUri() {
    return uri;
  }

  public LoadBalancerMethod getMethod() {
    return method;
  }

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
