package com.hubspot.singularity.api.webhooks;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public enum WebhookType {
  TASK, REQUEST, DEPLOY
}
