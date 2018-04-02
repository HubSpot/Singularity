package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.expiring.SingularityExpiringBounce;
import com.hubspot.singularity.expiring.SingularityExpiringPause;
import com.hubspot.singularity.expiring.SingularityExpiringScale;
import com.hubspot.singularity.expiring.SingularityExpiringSkipHealthchecks;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Holds a SingularityRequest and its current state")
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
  private final Optional<SingularityTaskIdsByStatus> taskIds;
  private final Optional<SingularityRequestHistory> lastHistory;
  private final Optional<SingularityTaskIdHistory> mostRecentTask;

  public SingularityRequestParent(SingularityRequest request, RequestState state) {
    this(request, state, Optional.absent());
  }

  public SingularityRequestParent(SingularityRequest request, RequestState state, Optional<SingularityRequestDeployState> requestDeployState) {
    this(request, state, requestDeployState, Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent());
  }

  @JsonCreator
  public SingularityRequestParent(@JsonProperty("request") SingularityRequest request,
                                  @JsonProperty("state") RequestState state,
                                  @JsonProperty("requestDeployState") Optional<SingularityRequestDeployState> requestDeployState,
                                  @JsonProperty("activeDeploy") Optional<SingularityDeploy> activeDeploy,
                                  @JsonProperty("pendingDeploy") Optional<SingularityDeploy> pendingDeploy,
                                  @JsonProperty("pendingDeployState") Optional<SingularityPendingDeploy> pendingDeployState,
                                  @JsonProperty("expiringBounce") Optional<SingularityExpiringBounce> expiringBounce,
                                  @JsonProperty("expiringPause") Optional<SingularityExpiringPause> expiringPause,
                                  @JsonProperty("expiringScale") Optional<SingularityExpiringScale> expiringScale,
                                  @JsonProperty("expiringSkipHealthchecks") Optional<SingularityExpiringSkipHealthchecks> expiringSkipHealthchecks,
                                  @JsonProperty("taskIds") Optional<SingularityTaskIdsByStatus> taskIds,
                                  @JsonProperty("lastHistory") Optional<SingularityRequestHistory> lastHistory,
                                  @JsonProperty("mostRecentTask") Optional<SingularityTaskIdHistory> mostRecentTask) {
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
    this.taskIds = taskIds;
    this.lastHistory = lastHistory;
    this.mostRecentTask = mostRecentTask;
  }


  @Schema(description = "Current state of the request")
  public RequestState getState() {
    return state;
  }

  @Schema(description = "Full data for the request, a parent object for any deployed task")
  public SingularityRequest getRequest() {
    return request;
  }

  @Schema(description = "Data about any active and/or pending deploys for this request", nullable = true)
  public Optional<SingularityRequestDeployState> getRequestDeployState() {
    return requestDeployState;
  }

  @Schema(description = "Settings for the active deploy, may not contain the full deploy json", nullable = true)
  public Optional<SingularityDeploy> getActiveDeploy() {
    return activeDeploy;
  }

  @Schema(description = "Settings for a pending/in-progress deploy, may not contain the full deploy json", nullable = true)
  public Optional<SingularityDeploy> getPendingDeploy() {
    return pendingDeploy;
  }

  @Schema(description = "State of the pending deploy if one is present", nullable = true)
  public Optional<SingularityPendingDeploy> getPendingDeployState() {
    return pendingDeployState;
  }

  @Schema(description = "Expiring bounce action details if one is active", nullable = true)
  public Optional<SingularityExpiringBounce> getExpiringBounce() {
    return expiringBounce;
  }

  @Schema(description = "Details about an expiring pause if one is present", nullable = true)
  public Optional<SingularityExpiringPause> getExpiringPause() {
    return expiringPause;
  }

  @Schema(description = "Details about a scale action that will eventually revert, if one is present", nullable = true)
  public Optional<SingularityExpiringScale> getExpiringScale() {
    return expiringScale;
  }

  @Schema(description = "Details about a skip healthchecks action that will eventually revert", nullable = true)
  public Optional<SingularityExpiringSkipHealthchecks> getExpiringSkipHealthchecks() {
    return expiringSkipHealthchecks;
  }

  @Schema(description = "A list of active and pending task ids, separated by status", nullable = true)
  public Optional<SingularityTaskIdsByStatus> getTaskIds() {
    return taskIds;
  }

  @Schema(description = "The most recent historical update to this request", nullable = true)
  public Optional<SingularityRequestHistory> getLastHistory() {
    return lastHistory;
  }

  @Schema(description = "The most recent completed task for this request", nullable = true)
  public Optional<SingularityTaskIdHistory> getMostRecentTask() {
    return mostRecentTask;
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
        ", taskIds=" + taskIds +
        ", lastHistory=" + lastHistory +
        ", mostRecentTask=" + mostRecentTask +
        '}';
  }
}
