package com.hubspot.singularity.api.task;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public enum SimplifiedTaskState {
  UNKNOWN, WAITING, RUNNING, DONE
}
