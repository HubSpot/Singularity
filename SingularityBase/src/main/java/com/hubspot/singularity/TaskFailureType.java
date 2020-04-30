package com.hubspot.singularity;

public enum TaskFailureType {
  OOM,
  OUT_OF_DISK_SPACE,
  BAD_EXIT_CODE,
  UNEXPECTED_EXIT,
  LOST_SLAVE,
  MESOS_ERROR,
  STARTUP_FAILURE
}
