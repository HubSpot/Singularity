package com.hubspot.singularity.api;

import com.google.common.base.Optional;

public abstract class SingularityExpiringRequestParent {

  private final Optional<Long> durationMillis;
  private final Optional<String> actionId;
  private final Optional<String> message;

  public SingularityExpiringRequestParent(Optional<Long> durationMillis, Optional<String> actionId, Optional<String> message) {
    this.actionId = actionId;
    this.durationMillis = durationMillis;
    this.message = message;
  }

  public Optional<String> getMessage() {
    return message;
  }

  public Optional<String> getActionId() {
    return actionId;
  }

  public Optional<Long> getDurationMillis() {
    return durationMillis;
  }

  @Override
  public String toString() {
    return "SingularityExpiringRequestParent [durationMillis=" + durationMillis + ", actionId=" + actionId + ", message=" + message + "]";
  }

}
