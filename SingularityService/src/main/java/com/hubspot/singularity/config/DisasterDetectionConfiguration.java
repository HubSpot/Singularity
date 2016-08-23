package com.hubspot.singularity.config;

import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DisasterDetectionConfiguration {

  @JsonProperty
  private boolean enabled = false;

  @JsonProperty
  @NotNull
  private long runEveryMillis = TimeUnit.SECONDS.toMillis(15);


  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public long getRunEveryMillis() {
    return runEveryMillis;
  }

  public void setRunEveryMillis(long runEveryMillis) {
    this.runEveryMillis = runEveryMillis;
  }
}
