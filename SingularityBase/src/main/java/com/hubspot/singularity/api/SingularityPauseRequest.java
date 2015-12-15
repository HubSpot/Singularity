package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityPauseRequest {

  private final Optional<Boolean> killTasks;
  private final Optional<Long> durationMillis;

  @JsonCreator
  public SingularityPauseRequest(
      @JsonProperty("killTasks") Optional<Boolean> killTasks,
      @JsonProperty("durationMillis") Optional<Long> durationMillis) {
    this.killTasks = killTasks;
    this.durationMillis = durationMillis;
  }

  public Optional<Boolean> getKillTasks() {
    return killTasks;
  }

  public Optional<Long> getDurationMillis() {
    return durationMillis;
  }

  @Override
  public String toString() {
    return "SingularityPauseRequest [killTasks=" + killTasks + ", durationMillis=" + durationMillis + "]";
  }

}
