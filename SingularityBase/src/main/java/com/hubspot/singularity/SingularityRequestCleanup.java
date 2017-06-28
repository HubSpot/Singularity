package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityRequestCleanup {

  private final Optional<String> user;
  private final RequestCleanupType cleanupType;
  private final Optional<Boolean> killTasks;
  private final Optional<Boolean> removeFromLoadBalancer;
  private final Optional<Boolean> skipHealthchecks;
  private final Optional<String> deployId;
  private final long timestamp;
  private final String requestId;
  private final Optional<String> message;
  private final Optional<String> actionId;
  private final Optional<SingularityShellCommand> runShellCommandBeforeKill;

  @JsonCreator
  public SingularityRequestCleanup(@JsonProperty("user") Optional<String> user,
                                   @JsonProperty("cleanupType") RequestCleanupType cleanupType,
                                   @JsonProperty("timestamp") long timestamp,
                                   @JsonProperty("killTasks") Optional<Boolean> killTasks,
                                   @JsonProperty("removeFromLoadBalancer") Optional<Boolean> removeFromLoadBalancer,
                                   @JsonProperty("requestId") String requestId,
                                   @JsonProperty("deployId") Optional<String> deployId,
                                   @JsonProperty("skipHealthchecks") Optional<Boolean> skipHealthchecks,
                                   @JsonProperty("message") Optional<String> message,
                                   @JsonProperty("actionId") Optional<String> actionId,
                                   @JsonProperty("runShellCommandBeforeKill") Optional<SingularityShellCommand> runShellCommandBeforeKill) {
    this.user = user;
    this.cleanupType = cleanupType;
    this.timestamp = timestamp;
    this.requestId = requestId;
    this.deployId = deployId;
    this.killTasks = killTasks;
    this.removeFromLoadBalancer = removeFromLoadBalancer;
    this.skipHealthchecks = skipHealthchecks;
    this.actionId = actionId;
    this.message = message;
    this.runShellCommandBeforeKill = runShellCommandBeforeKill;
  }

  public Optional<Boolean> getSkipHealthchecks() {
    return skipHealthchecks;
  }

  public Optional<Boolean> getKillTasks() {
    return killTasks;
  }

  public Optional<Boolean> getRemoveFromLoadBalancer() {
    return removeFromLoadBalancer;
  }

  public String getRequestId() {
    return requestId;
  }

  public Optional<String> getUser() {
    return user;
  }

  public RequestCleanupType getCleanupType() {
    return cleanupType;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public Optional<String> getDeployId() {
    return deployId;
  }

  public Optional<String> getMessage() {
    return message;
  }

  public Optional<String> getActionId() {
    return actionId;
  }

  public Optional<SingularityShellCommand> getRunShellCommandBeforeKill() {
    return runShellCommandBeforeKill;
  }

  @Override
  public String toString() {
    return "SingularityRequestCleanup{" +
        "user=" + user +
        ", cleanupType=" + cleanupType +
        ", killTasks=" + killTasks +
        ", removeFromLoadBalancer=" + removeFromLoadBalancer +
        ", skipHealthchecks=" + skipHealthchecks +
        ", deployId=" + deployId +
        ", timestamp=" + timestamp +
        ", requestId='" + requestId + '\'' +
        ", message=" + message +
        ", actionId=" + actionId +
        ", runShellCommandBeforeKill=" + runShellCommandBeforeKill +
        '}';
  }
}
