package com.hubspot.singularity.expiring;

import com.google.common.base.Optional;
import com.hubspot.singularity.api.SingularityExpiringRequestParent;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    title = "Represents a future action on an object",
    subTypes = {
        SingularityExpiringMachineState.class,
        SingularityExpiringRequestActionParent.class
    }
)
public abstract class SingularityExpiringParent<T extends SingularityExpiringRequestParent> {

  private final Optional<String> user;
  private final long startMillis;
  private final String actionId;
  private final T expiringAPIRequestObject;

  public SingularityExpiringParent(T expiringAPIRequestObject, Optional<String> user, long startMillis, String actionId) {
    this.expiringAPIRequestObject = expiringAPIRequestObject;
    this.user = user;
    this.startMillis = startMillis;
    this.actionId = actionId;
  }

  @Schema(description = "Object associated with the future action")
  public T getExpiringAPIRequestObject() {
    return expiringAPIRequestObject;
  }

  @Schema(description = "User who initially triggered the future action", nullable = true)
  public Optional<String> getUser() {
    return user;
  }

  @Schema(description = "Time the future action was created")
  public long getStartMillis() {
    return startMillis;
  }

  @Schema(description = "A unique id for this future action")
  public String getActionId() {
    return actionId;
  }

  @Override
  public String toString() {
    return "SingularityExpiringParent{" +
        "user=" + user +
        ", startMillis=" + startMillis +
        ", actionId='" + actionId + '\'' +
        ", expiringAPIRequestObject=" + expiringAPIRequestObject +
        '}';
  }
}
