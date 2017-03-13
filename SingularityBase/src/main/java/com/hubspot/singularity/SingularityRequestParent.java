package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.expiring.SingularityExpiringBounce;
import com.hubspot.singularity.expiring.SingularityExpiringPause;
import com.hubspot.singularity.expiring.SingularityExpiringScale;
import com.hubspot.singularity.expiring.SingularityExpiringSkipHealthchecks;

public class SingularityRequestParent {

  private final SingularityRequest request;
  private final RequestState state;
  private final Optional<SingularityRequestDeployState> requestDeployState;
  private final Optional<SingularityDeploy> activeDeploy;
  private final Optional<SingularityDeploy> pendingDeploy;
  private final Optional<SingularityPendingDeploy> pendingDeployState;
  private final Optional<SingularityExpiringBounce> expiringBounce;
  private final Optional<SingularityExpiringPause> expiringPause;
  private final Optional<SingularityExpiringScale> expiringScale;
  private final Optional<SingularityExpiringSkipHealthchecks> expiringSkipHealthchecks;

  public SingularityRequestParent(SingularityRequest request, RequestState state) {
    this(request, state, Optional.<SingularityRequestDeployState> absent());
  }

  public SingularityRequestParent(SingularityRequest request, RequestState state, Optional<SingularityRequestDeployState> requestDeployState) {
    this(request, state, requestDeployState, Optional.<SingularityDeploy> absent(), Optional.<SingularityDeploy> absent(),
        Optional.<SingularityPendingDeploy> absent(), Optional.<SingularityExpiringBounce> absent(), Optional.<SingularityExpiringPause> absent(),
        Optional.<SingularityExpiringScale> absent(), Optional.<SingularityExpiringSkipHealthchecks> absent());
  }

  @JsonCreator
  public SingularityRequestParent(@JsonProperty("request") SingularityRequest request, @JsonProperty("state") RequestState state,
      @JsonProperty("requestDeployState") Optional<SingularityRequestDeployState> requestDeployState,
      @JsonProperty("activeDeploy") Optional<SingularityDeploy> activeDeploy, @JsonProperty("pendingDeploy") Optional<SingularityDeploy> pendingDeploy,
      @JsonProperty("pendingDeployState") Optional<SingularityPendingDeploy> pendingDeployState, @JsonProperty("expiringBounce") Optional<SingularityExpiringBounce> expiringBounce,
      @JsonProperty("expiringPause") Optional<SingularityExpiringPause> expiringPause, @JsonProperty("expiringScale") Optional<SingularityExpiringScale> expiringScale,
      @JsonProperty("expiringSkipHealthchecks") Optional<SingularityExpiringSkipHealthchecks> expiringSkipHealthchecks) {
    this.request = request;
    this.state = state;
    this.requestDeployState = requestDeployState;
    this.activeDeploy = activeDeploy;
    this.pendingDeploy = pendingDeploy;
    this.pendingDeployState = pendingDeployState;
    this.expiringBounce = expiringBounce;
    this.expiringPause = expiringPause;
    this.expiringScale = expiringScale;
    this.expiringSkipHealthchecks = expiringSkipHealthchecks;
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

  public Optional<SingularityExpiringBounce> getExpiringBounce() {
    return expiringBounce;
  }

  public Optional<SingularityExpiringPause> getExpiringPause() {
    return expiringPause;
  }

  public Optional<SingularityExpiringScale> getExpiringScale() {
    return expiringScale;
  }

  public Optional<SingularityExpiringSkipHealthchecks> getExpiringSkipHealthchecks() {
    return expiringSkipHealthchecks;
  }

  @Override
  public String toString() {
    return "SingularityRequestParent{" +
        "request=" + request +
        ", state=" + state +
        ", requestDeployState=" + requestDeployState +
        ", activeDeploy=" + activeDeploy +
        ", pendingDeploy=" + pendingDeploy +
        ", pendingDeployState=" + pendingDeployState +
        ", expiringBounce=" + expiringBounce +
        ", expiringPause=" + expiringPause +
        ", expiringScale=" + expiringScale +
        ", expiringSkipHealthchecks=" + expiringSkipHealthchecks +
        '}';
  }
}
