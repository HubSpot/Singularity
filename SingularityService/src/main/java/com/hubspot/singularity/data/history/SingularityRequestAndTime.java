package com.hubspot.singularity.data.history;

import com.hubspot.singularity.SingularityRequest;

public class SingularityRequestAndTime {
  private final SingularityRequest request;
  private final long createdAt;

  public SingularityRequestAndTime(SingularityRequest request, long createdAt) {
    this.request = request;
    this.createdAt = createdAt;
  }

  public SingularityRequest getRequest() {
    return request;
  }

  public long getCreatedAt() {
    return createdAt;
  }
}
