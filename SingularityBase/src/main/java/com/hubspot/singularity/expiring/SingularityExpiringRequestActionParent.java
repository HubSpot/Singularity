package com.hubspot.singularity.expiring;

import com.google.common.base.Optional;
import com.hubspot.singularity.api.SingularityExpiringRequestParent;

public abstract class SingularityExpiringRequestActionParent<T extends SingularityExpiringRequestParent> extends SingularityExpiringParent<T> {

  private final String requestId;

  public SingularityExpiringRequestActionParent(T expiringAPIRequestObject, Optional<String> user, long startMillis, String actionId, String requestId) {
    super(expiringAPIRequestObject, user, startMillis, actionId);
    this.requestId = requestId;
  }

  public String getRequestId() {
    return requestId;
  }

  @Override
  public String toString() {
    return "SingularityExpiringParent [requestId=" + requestId + ", user=" + getUser() + ", startMillis=" + getStartMillis() + ", actionId=" + getActionId() + ", expiringAPIRequestObject=" + getExpiringAPIRequestObject() + "]";
  }

}
