package com.hubspot.singularity;

import java.io.IOException;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;

public class SingularityLoadBalancerUpdate extends SingularityJsonObject {

  private final BaragonRequestState loadBalancerState;
  private final Optional<String> message;
  private final long timestamp;
  private final Optional<String> uri;
  private final LoadBalancerMethod method;
  private final LoadBalancerRequestId loadBalancerRequestId;

  public static SingularityLoadBalancerUpdate fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityLoadBalancerUpdate.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }

  public enum LoadBalancerMethod {
    PRE_ENQUEUE, ENQUEUE, CHECK_STATE, CANCEL;
  }

  @JsonCreator
  public SingularityLoadBalancerUpdate(@JsonProperty("state") BaragonRequestState loadBalancerState, @JsonProperty("loadBalancerRequestId") LoadBalancerRequestId loadBalancerRequestId, @JsonProperty("message") Optional<String> message,
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

  @Override
  public String toString() {
    return "SingularityLoadBalancerUpdate [loadBalancerState=" + loadBalancerState + ", message=" + message + ", timestamp=" + timestamp + ", uri=" + uri + ", method=" + method + ", loadBalancerRequestId=" + loadBalancerRequestId + "]";
  }

}
