package com.hubspot.singularity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public enum WebhookType {
  TASK,
  REQUEST,
  DEPLOY,
  CRASHLOOP,
  ELEVATED_ACCESS
}
