package com.hubspot.singularity;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SingularityRequestDeployState extends SingularityJsonObject {

  private final String requestId;
  
  private final Optional<SingularityDeployMarker> activeDeploy;
  private final Optional<SingularityDeployMarker> pendingDeploy;
  
  public static SingularityRequestDeployState fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityRequestDeployState.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }
  
  @JsonCreator
  public SingularityRequestDeployState(@JsonProperty("requestId") String requestId, @JsonProperty("activeDeploy") Optional<SingularityDeployMarker> activeDeploy, @JsonProperty("pendingDeploy") Optional<SingularityDeployMarker> pendingDeploy) {
    this.requestId = requestId;
    this.activeDeploy = activeDeploy;
    this.pendingDeploy = pendingDeploy;
  }

  public String getRequestId() {
    return requestId;
  }

  public Optional<SingularityDeployMarker> getActiveDeploy() {
    return activeDeploy;
  }
  
  public Optional<SingularityDeployMarker> getPendingDeploy() {
    return pendingDeploy;
  }

  @Override
  public String toString() {
    return "SingularityRequestDeployState [requestId=" + requestId + ", activeDeploy=" + activeDeploy + ", pendingDeploy=" + pendingDeploy + "]";
  }
  
}
