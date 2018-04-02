package com.hubspot.singularity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public enum ScheduleType {
  CRON, QUARTZ, RFC5545
}
