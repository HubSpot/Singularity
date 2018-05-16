package com.hubspot.singularity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public enum SlavePlacement {
  SEPARATE, OPTIMISTIC, GREEDY, SEPARATE_BY_DEPLOY, SEPARATE_BY_REQUEST, SPREAD_ALL_SLAVES
}
