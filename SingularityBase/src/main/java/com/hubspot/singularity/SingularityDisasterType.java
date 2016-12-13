package com.hubspot.singularity;

public enum SingularityDisasterType {
  EXCESSIVE_TASK_LAG(true), LOST_SLAVES(false), LOST_TASKS(false), USER_INITIATED(false);

  private boolean automaticallyClearable;

  SingularityDisasterType(boolean automaticallyClearable) {
    this.automaticallyClearable = automaticallyClearable;
  }

  public boolean isAutomaticallyClearable() {
    return automaticallyClearable;
  }
}
