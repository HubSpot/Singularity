package com.hubspot.singularity;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityDeployFailure {
  private final SingularityDeployFailureReason reason;
  private final Optional<SingularityTaskId> taskId;
  private final Optional<String> message;

  public SingularityDeployFailure(SingularityDeployFailureReason reason) {
    this(reason, Optional.<SingularityTaskId>absent(), Optional.<String>absent());
  }

  public static List<SingularityDeployFailure> lbUpdateFailed() {
    return Collections.singletonList(new SingularityDeployFailure(SingularityDeployFailureReason.LOAD_BALANCER_UPDATE_FAILED));
  }

  public static List<SingularityDeployFailure> failedToSave() {
    return Collections.singletonList(new SingularityDeployFailure(SingularityDeployFailureReason.FAILED_TO_SAVE_DEPLOY_STATE));
  }

  public static List<SingularityDeployFailure> deployRemoved() {
    return Collections.singletonList(new SingularityDeployFailure(SingularityDeployFailureReason.PENDING_DEPLOY_REMOVED));
  }

  @JsonCreator
  public SingularityDeployFailure(@JsonProperty("reason") SingularityDeployFailureReason reason,
    @JsonProperty("taskId") Optional<SingularityTaskId> taskId,
    @JsonProperty("message") Optional<String> message) {
    this.reason = reason;
    this.taskId = taskId;
    this.message = message;
  }

  public SingularityDeployFailureReason getReason() {
    return reason;
  }

  public Optional<SingularityTaskId> getTaskId() {
    return taskId;
  }

  public Optional<String> getMessage() {
    return message;
  }

  @Override public String toString() {
    return "SingularityDeployFailure{" +
      "reason=" + reason +
      ", taskId=" + taskId +
      ", message=" + message +
      '}';
  }
}
