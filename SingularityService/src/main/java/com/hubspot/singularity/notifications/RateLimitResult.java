package com.hubspot.singularity.notifications;

public enum RateLimitResult {
  SEND_MAIL, DONT_SEND_MAIL_IN_COOLDOWN, SEND_COOLDOWN_STARTED_MAIL
}
