package com.hubspot.singularity.api.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public enum LoadBalancerMethod {
  PRE_ENQUEUE, ENQUEUE, CHECK_STATE, CANCEL, DELETE;
}
