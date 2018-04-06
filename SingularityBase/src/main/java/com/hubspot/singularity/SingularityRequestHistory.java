package com.hubspot.singularity;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.ComparisonChain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Describes an update or action on a Singularity request")
public class SingularityRequestHistory implements Comparable<SingularityRequestHistory>, SingularityHistoryItem {

  private final long createdAt;
  private final Optional<String> user;
  private final RequestHistoryType eventType;
  private final SingularityRequest request;
  private final Optional<String> message;

  @Schema
  public enum RequestHistoryType {
    CREATED, UPDATED, DELETING, DELETED, PAUSED, UNPAUSED, ENTERED_COOLDOWN, EXITED_COOLDOWN, FINISHED, DEPLOYED_TO_UNPAUSE, BOUNCED, SCALED, SCALE_REVERTED;
  }

  @JsonCreator
  public SingularityRequestHistory(@JsonProperty("createdAt") long createdAt, @JsonProperty("user") Optional<String> user,
                                   @JsonProperty("eventType") RequestHistoryType eventType, @JsonProperty("request") SingularityRequest request,
                                   @JsonProperty("message") Optional<String> message) {
    this.createdAt = createdAt;
    this.user = user;
    this.eventType = eventType;
    this.request = request;
    this.message = message;
  }

  @Override
  public int compareTo(SingularityRequestHistory o) {
    return ComparisonChain
        .start()
        .compare(o.getCreatedAt(), createdAt)
        .compare(request.getId(), o.getRequest().getId())
        .result();
  }

  @Schema(description = "The time the request update occured")
  public long getCreatedAt() {
    return createdAt;
  }

  @Schema(description = "The user associated with the request update", nullable = true)
  public Optional<String> getUser() {
    return user;
  }

  @Deprecated
  @JsonIgnore
  @Schema(description = "The type of request history update")
  public RequestHistoryType getState() {
    return eventType;
  }

  @Schema(description = "The type of request history update")
  public RequestHistoryType getEventType() {
    return eventType;
  }

  @Schema(description = "The full data of the request after being updated")
  public SingularityRequest getRequest() {
    return request;
  }

  @Schema(description = "An optional message accompanying the update", nullable = true)
  public Optional<String> getMessage() {
    return message;
  }

  @Override
  @JsonIgnore
  public long getCreateTimestampForCalculatingHistoryAge() {
    return createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityRequestHistory that = (SingularityRequestHistory) o;
    return createdAt == that.createdAt &&
        Objects.equals(user, that.user) &&
        eventType == that.eventType &&
        Objects.equals(request, that.request) &&
        Objects.equals(message, that.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdAt, user, eventType, request, message);
  }

  @Override
  public String toString() {
    return "SingularityRequestHistory{" +
        "createdAt=" + createdAt +
        ", user=" + user +
        ", eventType=" + eventType +
        ", request=" + request +
        ", message=" + message +
        '}';
  }
}
