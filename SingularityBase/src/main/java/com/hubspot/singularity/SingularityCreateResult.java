package com.hubspot.singularity;

public enum SingularityCreateResult {
  CREATED(true), EXISTED(true), UNKNOWN(false);

  private final boolean successful;

  private SingularityCreateResult(final boolean successful) {
    this.successful = successful;
  }

  public boolean isSuccessful() {
    return successful;
  }
}
