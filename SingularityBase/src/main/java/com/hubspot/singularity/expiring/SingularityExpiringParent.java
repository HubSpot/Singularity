package com.hubspot.singularity.expiring;

import com.google.common.base.Optional;

public abstract class SingularityExpiringParent {

  private final long durationMillis;
  private final String requestId;
  private final Optional<String> user;
  private final long startMillis;

  public SingularityExpiringParent(long durationMillis, String requestId, Optional<String> user, long startMillis) {
    this.durationMillis = durationMillis;
    this.requestId = requestId;
    this.user = user;
    this.startMillis = startMillis;
  }

  public long getDurationMillis() {
    return durationMillis;
  }

  public String getRequestId() {
    return requestId;
  }

  public Optional<String> getUser() {
    return user;
  }

  public long getStartMillis() {
    return startMillis;
  }

  @Override
  public String toString() {
    return "SingularityExpiringParent [durationMillis=" + durationMillis + ", requestId=" + requestId + ", user=" + user + ", startMillis=" + startMillis + "]";
  }

}
