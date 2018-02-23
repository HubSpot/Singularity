package com.hubspot.singularity;

import com.wordnik.swagger.annotations.ApiModel;

@ApiModel
public enum WebhookType {
  TASK, REQUEST, DEPLOY
}
