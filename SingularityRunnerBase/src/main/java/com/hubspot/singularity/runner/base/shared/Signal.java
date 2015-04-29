package com.hubspot.singularity.runner.base.shared;

public enum Signal {
  SIGTERM(15), SIGKILL(9), CHECK(0);

  private final int code;

  private Signal(int code) {
    this.code = code;
  }

  public int getCode() {
    return code;
  }

}
