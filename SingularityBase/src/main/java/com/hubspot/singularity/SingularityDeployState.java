package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SingularityDeployState extends SingularityJsonObject {

  private final String requestId;
  
  private final Optional<SingularityDeployMarker> activeDeploy;
  private final Optional<SingularityDeployMarker> pendingDeploy;
  
  @JsonCreator
  public SingularityDeployState(@JsonProperty("requestId") String requestId, @JsonProperty("activeDeploy") Optional<SingularityDeployMarker> activeDeploy, @JsonProperty("pendingDeploy") Optional<SingularityDeployMarker> pendingDeploy) {
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
  
  public static SingularityDeployState fromBytes(byte[] bytes, ObjectMapper objectMapper) throws Exception {
    return objectMapper.readValue(bytes, SingularityDeployState.class);
  }

}
