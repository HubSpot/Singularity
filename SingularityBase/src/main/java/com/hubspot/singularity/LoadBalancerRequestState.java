package com.hubspot.singularity;

import com.hubspot.baragon.models.BaragonRequestState;

public enum LoadBalancerRequestState {
  UNKNOWN(false),
  FAILED(false),
  WAITING(true),
  SUCCESS(false),
  CANCELING(true),
  CANCELED(false),
  INVALID_REQUEST_NOOP(false);

  private final boolean inProgress;

  LoadBalancerRequestState(boolean inProgress) {
    this.inProgress = inProgress;
  }

  public boolean isInProgress() {
    return inProgress;
  }

  public static LoadBalancerRequestState fromBaragonRequestState(
    BaragonRequestState baragonRequestState
  ) {
    switch (baragonRequestState) {
      case WAITING:
        return WAITING;
      case SUCCESS:
        return SUCCESS;
      case UNKNOWN:
        return UNKNOWN;
      case FAILED:
        return FAILED;
      case CANCELED:
        return CANCELED;
      case CANCELING:
        return CANCELING;
      case INVALID_REQUEST_NOOP:
        return INVALID_REQUEST_NOOP;
      default:
        return null;
    }
  }
}
