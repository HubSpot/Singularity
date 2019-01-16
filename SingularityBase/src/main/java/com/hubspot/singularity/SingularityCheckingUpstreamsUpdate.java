package com.hubspot.singularity;

import com.google.common.base.Optional;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.baragon.models.BaragonServiceState;

public class SingularityCheckingUpstreamsUpdate {
  private final BaragonRequestState baragonRequestState;
  private final Optional<BaragonServiceState> baragonServiceState;
  private final String singularityRequestId;

  public SingularityCheckingUpstreamsUpdate (BaragonRequestState baragonRequestState,Optional<BaragonServiceState> baragonServiceState, String singularityRequest) {
    this.baragonRequestState = baragonRequestState;
    this.baragonServiceState = baragonServiceState;
    this.singularityRequestId = singularityRequest;
  }

  public String getSingularityRequestId() {
    return singularityRequestId;
  }

  public BaragonRequestState getBaragonRequestState() {
    return baragonRequestState;
  }

  public Optional<BaragonServiceState> getBaragonServiceState() {
    return baragonServiceState;
  }

  @Override
  public String toString() {
    return "SingularityCheckingUpstreamsUpdate{" +
        "baragonRequestState=" + baragonRequestState +
        ", baragonServiceState=" + baragonServiceState +
        ", singularityRequestId=" + singularityRequestId +
        '}';
  }
}
