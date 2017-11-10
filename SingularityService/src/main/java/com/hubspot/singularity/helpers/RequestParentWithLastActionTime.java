package com.hubspot.singularity.helpers;

import java.util.Objects;

import com.google.common.primitives.Longs;
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
    return Longs.compare(other.getLastActionTime(), lastActionTime);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof RequestParentWithLastActionTime) {
      final RequestParentWithLastActionTime that = (RequestParentWithLastActionTime) obj;
      return Objects.equals(this.lastActionTime, that.lastActionTime) &&
          Objects.equals(this.requestWithState, that.requestWithState);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(requestWithState, lastActionTime);
  }
}
