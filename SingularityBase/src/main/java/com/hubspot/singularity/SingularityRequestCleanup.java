package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents an action requiring tasks for a request to possibly be cleaned or replaced")
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

  @Schema(description = "If `true`, skip health checks for new tasks created", nullable = true)
  public Optional<Boolean> getSkipHealthchecks() {
    return skipHealthchecks;
  }

  @Schema(description = "Override the system default behavior for immediately kiiling a task (relevant for pause-related cleanups)", nullable = true)
  public Optional<Boolean> getKillTasks() {
    return killTasks;
  }

  @Schema(description = "For deletes, remove the service from the load balancer when all tasks are cleaned", nullable = true)
  public Optional<Boolean> getRemoveFromLoadBalancer() {
    return removeFromLoadBalancer;
  }

  @Schema(description = "The request this cleanup relates to")
  public String getRequestId() {
    return requestId;
  }

  @Schema(description = "The user that triggered this cleanup", nullable = true)
  public Optional<String> getUser() {
    return user;
  }

  @Schema(description = "The type of cleanup")
  public RequestCleanupType getCleanupType() {
    return cleanupType;
  }

  @Schema(description = "The time this cleanup was created")
  public long getTimestamp() {
    return timestamp;
  }

  @Schema(description = "A specific deploy related to this cleanup", nullable = true)
  public Optional<String> getDeployId() {
    return deployId;
  }

  @Schema(description = "An optional message stating the reason for this cleanup", nullable = true)
  public Optional<String> getMessage() {
    return message;
  }

  @Schema(description = "An optional unique id for this cleanup action", nullable = true)
  public Optional<String> getActionId() {
    return actionId;
  }

  @Schema(description = "A shell command to run on tasks before they are killed", nullable = true)
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
