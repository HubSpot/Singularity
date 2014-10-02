package com.hubspot.singularity;

import java.io.IOException;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SingularityRequestParent extends SingularityJsonObject {

  private final SingularityRequest request;
  private final RequestState state;
  private final Optional<SingularityRequestDeployState> requestDeployState;
  private final Optional<SingularityDeploy> activeDeploy;
  private final Optional<SingularityDeploy> pendingDeploy;
  private final Optional<SingularityPendingDeploy> pendingDeployState;

  public SingularityRequestParent(SingularityRequest request, RequestState state) {
    this(request, state, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
  }

  public static SingularityRequestParent fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityRequestParent.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }

  @JsonCreator
  public SingularityRequestParent(@JsonProperty("request") SingularityRequest request, @JsonProperty("state") RequestState state, @JsonProperty("requestDeployState") Optional<SingularityRequestDeployState> requestDeployState,
      @JsonProperty("activeDeploy") Optional<SingularityDeploy> activeDeploy, @JsonProperty("pendingDeploy") Optional<SingularityDeploy> pendingDeploy, @JsonProperty("pendingDeployState") Optional<SingularityPendingDeploy> pendingDeployState) {
    this.request = request;
    this.state = state;
    this.requestDeployState = requestDeployState;
    this.activeDeploy = activeDeploy;
    this.pendingDeploy = pendingDeploy;
    this.pendingDeployState = pendingDeployState;
  }

  public RequestState getState() {
    return state;
  }

  public SingularityRequest getRequest() {
    return request;
  }

  public Optional<SingularityRequestDeployState> getRequestDeployState() {
    return requestDeployState;
  }

  public Optional<SingularityDeploy> getActiveDeploy() {
    return activeDeploy;
  }

  public Optional<SingularityDeploy> getPendingDeploy() {
    return pendingDeploy;
  }

  public Optional<SingularityPendingDeploy> getPendingDeployState() {
    return pendingDeployState;
  }

  @Override
  public String toString() {
    return "SingularityRequestParent [request=" + request + ", state=" + state + ", requestDeployState=" + requestDeployState + ", activeDeploy=" + activeDeploy + ", pendingDeploy=" + pendingDeploy + ", pendingDeployState="
        + pendingDeployState + "]";
  }

}
