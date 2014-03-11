package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SingularityDeployMarker extends SingularityJsonObject {

  private final String requestId;
  
  private final String deployId;

  private final long timestamp;
  private final Optional<String> user;
  
  @JsonCreator
  public SingularityDeployMarker(@JsonProperty("requestId") String requestId, @JsonProperty("deployId") String deployId, @JsonProperty("timestamp") long timestamp, @JsonProperty("user") Optional<String> user) {
    this.requestId = requestId;
    this.deployId = deployId;
    this.timestamp = timestamp;
    this.user = user;
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
  
  public static SingularityDeployMarker fromBytes(byte[] bytes, ObjectMapper objectMapper) throws Exception {
    return objectMapper.readValue(bytes, SingularityDeployMarker.class);
  }

}
