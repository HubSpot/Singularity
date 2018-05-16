package com.hubspot.singularity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public enum SingularityCreateResult {
  CREATED, EXISTED;
}
