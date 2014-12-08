package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ComparisonChain;

public class SingularityRequestHistory implements Comparable<SingularityRequestHistory> {

  private final long createdAt;
  private final Optional<String> user;
  private final RequestHistoryType eventType;
  private final SingularityRequest request;

  public enum RequestHistoryType {
    CREATED, UPDATED, DELETED, PAUSED, UNPAUSED, ENTERED_COOLDOWN, EXITED_COOLDOWN, FINISHED, DEPLOYED_TO_UNPAUSE;
  }

  @JsonCreator
  public SingularityRequestHistory(@JsonProperty("createdAt") long createdAt, @JsonProperty("user") Optional<String> user, @JsonProperty("eventType") RequestHistoryType eventType, @JsonProperty("request") SingularityRequest request) {
    this.createdAt = createdAt;
    this.user = user;
    this.eventType = eventType;
    this.request = request;
  }

  @Override
  public int compareTo(SingularityRequestHistory o) {
    return ComparisonChain
        .start()
        .compare(o.getCreatedAt(), createdAt)
        .compare(request.getId(), o.getRequest().getId())
        .result();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(createdAt, user, eventType, request);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || other.getClass() != this.getClass()) {
      return false;
    }

    SingularityRequestHistory that = (SingularityRequestHistory) other;
    return Objects.equal(this.createdAt, that.createdAt)
        && Objects.equal(this.user, that.user)
        && Objects.equal(this.eventType, that.eventType)
        && Objects.equal(this.request, that.request);
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public Optional<String> getUser() {
    return user;
  }

  @Deprecated
  @JsonIgnore
  public RequestHistoryType getState() {
    return eventType;
  }

  public RequestHistoryType getEventType() {
    return eventType;
  }

  public SingularityRequest getRequest() {
    return request;
  }

  @Override
  public String toString() {
    return "SingularityRequestHistory [createdAt=" + createdAt + ", user=" + user + ", eventType=" + eventType + ", request=" + request + "]";
  }

}
