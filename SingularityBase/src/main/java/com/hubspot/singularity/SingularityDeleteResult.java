package com.hubspot.singularity;

public enum SingularityDeleteResult {
  DELETED(true), DIDNT_EXIST(true), UNKNOWN(false);

  private final boolean successful;

  private SingularityDeleteResult(final boolean successful) {
    this.successful = successful;
  }

  public boolean isSuccessful() {
    return successful;
  }
}
