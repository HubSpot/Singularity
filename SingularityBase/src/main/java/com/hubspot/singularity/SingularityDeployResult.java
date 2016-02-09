package com.hubspot.singularity;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

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
    this.deployFailures = Objects.firstNonNull(deployFailures, Collections.<SingularityDeployFailure> emptyList());
    this.timestamp = timestamp;
  }

  public Optional<SingularityLoadBalancerUpdate> getLbUpdate() {
    return lbUpdate;
  }

  public Optional<String> getMessage() {
    return message;
  }

  public DeployState getDeployState() {
    return deployState;
  }

  public List<SingularityDeployFailure> getDeployFailures() {
    return deployFailures;
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return "SingularityDeployResult [deployState=" + deployState + ", lbUpdate=" + lbUpdate + ", message=" + message + ", deployFailures=" + deployFailures + ", timestamp=" + timestamp + "]";
  }

}
