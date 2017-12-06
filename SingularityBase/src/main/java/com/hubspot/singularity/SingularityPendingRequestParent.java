package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.expiring.SingularityExpiringBounce;
import com.hubspot.singularity.expiring.SingularityExpiringPause;
import com.hubspot.singularity.expiring.SingularityExpiringScale;
import com.hubspot.singularity.expiring.SingularityExpiringSkipHealthchecks;

public class SingularityPendingRequestParent extends SingularityRequestParent {

  private final SingularityPendingRequest pendingRequest;

  public static SingularityPendingRequestParent fromSingularityRequestParent(SingularityRequestParent singularityRequestParent, SingularityPendingRequest pendingRequest) {
    return new SingularityPendingRequestParent(singularityRequestParent.getRequest(), singularityRequestParent.getState(), singularityRequestParent.getRequestDeployState(),
        singularityRequestParent.getActiveDeploy(), singularityRequestParent.getPendingDeploy(), singularityRequestParent.getPendingDeployState(), pendingRequest,
        singularityRequestParent.getExpiringBounce(), singularityRequestParent.getExpiringPause(), singularityRequestParent.getExpiringScale(),
        singularityRequestParent.getExpiringSkipHealthchecks(), singularityRequestParent.getTaskIds(), singularityRequestParent.getLastHistory(), singularityRequestParent.getMostRecentTask());
  }

  @JsonCreator
  public SingularityPendingRequestParent(@JsonProperty("request") SingularityRequest request,
                                         @JsonProperty("state") RequestState state,
                                         @JsonProperty("requestDeployState") Optional<SingularityRequestDeployState> requestDeployState,
                                         @JsonProperty("activeDeploy") Optional<SingularityDeploy> activeDeploy,
                                         @JsonProperty("pendingDeploy") Optional<SingularityDeploy> pendingDeploy,
                                         @JsonProperty("pendingDeployState") Optional<SingularityPendingDeploy> pendingDeployState,
                                         @JsonProperty("pendingRequest") SingularityPendingRequest pendingRequest,
                                         @JsonProperty("expiringBounce") Optional<SingularityExpiringBounce> expiringBounce,
                                         @JsonProperty("expiringPause") Optional<SingularityExpiringPause> expiringPause,
                                         @JsonProperty("expiringScale") Optional<SingularityExpiringScale> expiringScale,
                                         @JsonProperty("expiringSkipHealthchecks") Optional<SingularityExpiringSkipHealthchecks> expiringSkipHealthchecks,
                                         @JsonProperty("taskIds") Optional<SingularityTaskIdsByStatus> taskIds,
                                         @JsonProperty("lastHistory") Optional<SingularityRequestHistory> lastHistory,
                                         @JsonProperty("mostRecentTask") Optional<SingularityTaskIdHistory> mostRecentTask) {
    super(request, state, requestDeployState, activeDeploy, pendingDeploy, pendingDeployState, expiringBounce, expiringPause, expiringScale, expiringSkipHealthchecks, taskIds, lastHistory, mostRecentTask);
    this.pendingRequest = pendingRequest;
  }

  public SingularityPendingRequest getPendingRequest() {
    return pendingRequest;
  }

  @Override
  public String toString() {
    return "SingularityPendingRequestParent{" +
        "pendingRequest=" + pendingRequest +
        "} " + super.toString();
  }
}
