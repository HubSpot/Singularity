package com.hubspot.singularity.api;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityPauseRequest {

  private final Optional<String> user;
  private final Optional<Boolean> killTasks;

  @JsonCreator
  public SingularityPauseRequest(@JsonProperty("user") Optional<String> user, @JsonProperty("killTasks") Optional<Boolean> killTasks) {
    this.user = user;
    this.killTasks = killTasks;
  }

  public Optional<String> getUser() {
    return user;
  }

  public Optional<Boolean> getKillTasks() {
    return killTasks;
  }

  @Override
  public String toString() {
    return "SingularityPauseRequest [user=" + user + ", killTasks=" + killTasks + "]";
  }

}
