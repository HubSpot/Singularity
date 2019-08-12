package com.hubspot.singularity;

import java.util.Optional;

import com.hubspot.baragon.models.BaragonServiceState;

public class SingularityCheckingUpstreamsUpdate {
  private final Optional<BaragonServiceState> baragonServiceState;
  private final String singularityRequestId;

  public SingularityCheckingUpstreamsUpdate (Optional<BaragonServiceState> baragonServiceState, String singularityRequest) {
    this.baragonServiceState = baragonServiceState;
    this.singularityRequestId = singularityRequest;
  }

  public String getSingularityRequestId() {
    return singularityRequestId;
  }

  public Optional<BaragonServiceState> getBaragonServiceState() {
    return baragonServiceState;
  }

  @Override
  public String toString() {
    return "SingularityCheckingUpstreamsUpdate{" +
        " baragonServiceState=" + baragonServiceState +
        ", singularityRequestId=" + singularityRequestId +
        '}';
  }
}
