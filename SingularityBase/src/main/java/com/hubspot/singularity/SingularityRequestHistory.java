package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityRequestHistory {

  private final long createdAt;
  private final Optional<String> user;
  private final RequestState state;
  private final SingularityRequest request;

  public enum RequestState {
    CREATED, UPDATED, DELETED, PAUSED, UNPAUSED;
  }

  @JsonCreator
  public SingularityRequestHistory(@JsonProperty("createdAt") long createdAt, @JsonProperty("user") Optional<String> user, @JsonProperty("state") RequestState state, @JsonProperty("request") SingularityRequest request) {
    this.createdAt = createdAt;
    this.user = user;
    this.state = state;
    this.request = request;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public Optional<String> getUser() {
    return user;
  }

  public RequestState getState() {
    return state;
  }

  public SingularityRequest getRequest() {
    return request;
  }
  
  @Override
  public String toString() {
    return "SingularityRequestHistory [createdAt=" + createdAt + ", user=" + user + ", state=" + state + ", request=" + request + "]";
  }

}
