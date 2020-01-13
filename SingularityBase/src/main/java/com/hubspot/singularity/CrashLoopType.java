package com.hubspot.singularity;

public enum CrashLoopType {
  FAST_FAILURE_LOOP("Instances are crashing > 5 times in the space of a minute"),
  SINGLE_INSTANCE_FAILURE_LOOP("A single instance is repeatedly crashing"),
  MULTI_INSTANCE_FAILURE("A significant percentage of instances are crashing within a short time period"),
  UNEXPECTED_EXITS("A long running task exits too often"),
  OOM("Too many ooms in a short time period"),
  SLOW_FAILURES("Slow but consistent failures over a period of hours"),
  STARTUP_FAILURE_LOOP("A task or tasks are failing to be replaced due to startup issues");

  private final String description;

  CrashLoopType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
