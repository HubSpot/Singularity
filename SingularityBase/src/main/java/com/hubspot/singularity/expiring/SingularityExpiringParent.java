package com.hubspot.singularity.expiring;

import com.google.common.base.Optional;
import com.hubspot.singularity.api.SingularityExpiringRequestParent;

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

  public T getExpiringAPIRequestObject() {
    return expiringAPIRequestObject;
  }

  public Optional<String> getUser() {
    return user;
  }

  public long getStartMillis() {
    return startMillis;
  }

  public String getActionId() {
    return actionId;
  }

  @Override
  public String toString() {
    return "SingularityExpiringParent [user=" + user + ", startMillis=" + startMillis + ", actionId=" + actionId + ", expiringAPIRequestObject=" + expiringAPIRequestObject + "]";
  }

}
