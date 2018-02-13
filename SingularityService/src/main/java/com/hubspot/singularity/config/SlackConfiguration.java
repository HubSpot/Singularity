package com.hubspot.singularity.config;

import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SlackConfiguration {

  @JsonProperty
  @NotNull
  private String slackApiToken;

  @JsonProperty
  private int slackMaxThreads = 3;

  @JsonProperty
  private int rateLimitAfterNotifications = 5;

  @JsonProperty
  private long rateLimtPeriodMillis = TimeUnit.MINUTES.toMillis(10);

  @JsonProperty
  private int slackClientRefreshRateSecs = 30;

  public String getSlackApiToken() {
    return slackApiToken;
  }

  public int getSlackMaxThreads() {
    return slackMaxThreads;
  }

  public int getRateLimitAfterNotifications() {
    return rateLimitAfterNotifications;
  }

  public long getRateLimtPeriodMillis() {
    return rateLimtPeriodMillis;
  }

  public int getSlackClientRefreshRateSecs() {
    return slackClientRefreshRateSecs;
  }
}
