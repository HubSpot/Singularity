package com.hubspot.singularity;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ComparisonChain;

public class SingularityDeployMarker extends SingularityJsonObject implements Comparable<SingularityDeployMarker> {

  private final String requestId;
  
  private final String deployId;

  private final long timestamp;
  private final Optional<String> user;
  
  public static SingularityDeployMarker fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityDeployMarker.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }
  
  @JsonCreator
  public SingularityDeployMarker(@JsonProperty("requestId") String requestId, @JsonProperty("deployId") String deployId, @JsonProperty("timestamp") long timestamp, @JsonProperty("user") Optional<String> user) {
    this.requestId = requestId;
    this.deployId = deployId;
    this.timestamp = timestamp;
    this.user = user;
  }

  @Override
  public int compareTo(SingularityDeployMarker o) {
    return ComparisonChain.start()
        .compare(timestamp, o.getTimestamp())
        .compare(deployId, o.getDeployId())
        .result();
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(requestId, deployId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SingularityDeployMarker other = (SingularityDeployMarker) obj;
    if (deployId == null) {
      if (other.deployId != null)
        return false;
    } else if (!deployId.equals(other.deployId))
      return false;
    if (requestId == null) {
      if (other.requestId != null)
        return false;
    } else if (!requestId.equals(other.requestId))
      return false;
    return true;
  }

  public String getRequestId() {
    return requestId;
  }

  public String getDeployId() {
    return deployId;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public Optional<String> getUser() {
    return user;
  }
  
  @JsonIgnore
  public String getLoadBalancerRequestId() {
    return String.format("%s-%s", getRequestId(), getDeployId());
  }
  
  @Override
  public String toString() {
    return "SingularityDeployMarker [requestId=" + requestId + ", deployId=" + deployId + ", timestamp=" + timestamp + ", user=" + user + "]";
  }

}
