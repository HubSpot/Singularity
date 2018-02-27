package com.hubspot.singularity;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The result of a deploy for a particular request")
public class SingularityDeployResult {

  private final DeployState deployState;
  private final Optional<SingularityLoadBalancerUpdate> lbUpdate;
  private final Optional<String> message;
  private final List<SingularityDeployFailure> deployFailures;
  private final long timestamp;

  public SingularityDeployResult(DeployState deployState) {
    this(deployState, Optional.<String> absent(), Optional.<SingularityLoadBalancerUpdate>absent(), Collections.<SingularityDeployFailure> emptyList(), System.currentTimeMillis());
  }

  public SingularityDeployResult(DeployState deployState, String message) {
    this(deployState, Optional.of(message), Optional.<SingularityLoadBalancerUpdate>absent(), Collections.<SingularityDeployFailure> emptyList(), System.currentTimeMillis());
  }

  public SingularityDeployResult(DeployState deployState, Optional<String> message, Optional<SingularityLoadBalancerUpdate> lbUpdate) {
    this(deployState, message, lbUpdate, Collections.<SingularityDeployFailure> emptyList(), System.currentTimeMillis());
  }

  public SingularityDeployResult(DeployState deployState, SingularityLoadBalancerUpdate lbUpdate, List<SingularityDeployFailure> deployFailures) {
    this(deployState, Optional.of(String.format("Load balancer had state %s%s", lbUpdate.getLoadBalancerState(),
      lbUpdate.getMessage().isPresent() && lbUpdate.getMessage().get().length() > 0 ? String.format(" (%s)", lbUpdate.getMessage().get()) : "")), Optional.of(lbUpdate), deployFailures, System.currentTimeMillis());
  }

  @JsonCreator
  public SingularityDeployResult(@JsonProperty("deployState") DeployState deployState, @JsonProperty("message") Optional<String> message, @JsonProperty("lbUpdate") Optional<SingularityLoadBalancerUpdate> lbUpdate,
    @JsonProperty("deployFailures") List<SingularityDeployFailure> deployFailures, @JsonProperty("timestamp") long timestamp) {
    this.deployState = deployState;
    this.lbUpdate = lbUpdate;
    this.message = message;
    this.deployFailures = deployFailures != null ? deployFailures : Collections.<SingularityDeployFailure> emptyList();
    this.timestamp = timestamp;
  }

  @Schema(description = "The result of the load balancer update, if one occured, for a load balanced request", nullable = true)
  public Optional<SingularityLoadBalancerUpdate> getLbUpdate() {
    return lbUpdate;
  }

  @Schema(description = "An optional message accompanying the deploy result", nullable = true)
  public Optional<String> getMessage() {
    return message;
  }

  @Schema(description = "The current state of the deploy")
  public DeployState getDeployState() {
    return deployState;
  }

  @Schema(description = "Details about a failed deploy", nullable = true)
  public List<SingularityDeployFailure> getDeployFailures() {
    return deployFailures;
  }

  @Schema(description = "The time of this deploy update")
  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return "SingularityDeployResult{" +
        "deployState=" + deployState +
        ", lbUpdate=" + lbUpdate +
        ", message=" + message +
        ", deployFailures=" + deployFailures +
        ", timestamp=" + timestamp +
        '}';
  }
}
