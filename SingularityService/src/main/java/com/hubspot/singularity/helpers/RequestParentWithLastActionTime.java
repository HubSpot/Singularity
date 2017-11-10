package com.hubspot.singularity.helpers;

import com.hubspot.singularity.SingularityRequestWithState;

public class RequestParentWithLastActionTime implements Comparable<RequestParentWithLastActionTime> {
  private final SingularityRequestWithState requestWithState;
  private final long lastActionTime;

  public RequestParentWithLastActionTime(SingularityRequestWithState requestWithState, long lastActionTime) {
    this.requestWithState = requestWithState;
    this.lastActionTime = lastActionTime;
  }

  public SingularityRequestWithState getRequestWithState() {
    return requestWithState;
  }

  public long getLastActionTime() {
    return lastActionTime;
  }

  @Override
  public int compareTo(RequestParentWithLastActionTime other) {
    return Long.compare(other.getLastActionTime(), lastActionTime);
  }
}
