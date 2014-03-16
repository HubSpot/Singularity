package com.hubspot.singularity;

import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;

public class SingularityRequestHistory {

  private final long createdAt;
  private final Optional<String> user;
  private final RequestState state;
  private final SingularityRequest request;

  public enum RequestState {
    CREATED, UPDATED, DELETED, PAUSED, UNPAUSED;
  }

  @JsonCreator
  public SingularityRequestHistory(@JsonProperty("createdAt") long createdAt, @JsonProperty("user") Optional<String> user, @JsonProperty("state") String state, @JsonProperty("request") SingularityRequest request) {
    this.createdAt = createdAt;
    this.user = user;
    this.state = RequestState.valueOf(state);
    this.request = request;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public Optional<String> getUser() {
    return user;
  }

  @JsonIgnore
  public RequestState getStateEnum() {
    return state;
  }

  public String getState() {
    return state.name();
  }

  public SingularityRequest getRequest() {
    return request;
  }

  public Map<String, String> formatJadeJson() {
    Date createdFormatted = new Date(createdAt);

    Map<String, String> formatted = Maps.newHashMap();
    formatted.put("state", state.toString());
    formatted.put("date", createdFormatted.toString());
    formatted.put("user", user.toString());
    formatted.put("request_id", request.getId());
    formatted.put("request_cmd", request.getCommand());

    return formatted;
  }

  @Override
  public String toString() {
    return "SingularityRequestHistory [createdAt=" + createdAt + ", user=" + user + ", state=" + state + ", request=" + request + "]";
  }

}
