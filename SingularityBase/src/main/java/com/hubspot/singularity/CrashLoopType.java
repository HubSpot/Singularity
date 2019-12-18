package com.hubspot.singularity;

public enum CrashLoopType {
  FAST_FAILURE_LOOP(""),
  SINGLE_INSTANCE_FAILURE_LOOP(""),
  SINGLE_INSTANCE_EXIT_LOOP(""),
  MULTI_INSTANCE_FAILURE_LOOP(""),
  MULTI_INSTANCE_EXIT_LOOP(""),
  OOM_LOOP(""),
  SLOW_FAILURE_LOOP(""),
  HEALTHCHECK_FAILURE_LOOP("");

  private final String description;

  CrashLoopType(String description) {
    this.description = description;
  }
}
