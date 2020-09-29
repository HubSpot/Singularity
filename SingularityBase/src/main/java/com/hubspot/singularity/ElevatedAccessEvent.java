package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ComparisonChain;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(
  description = "A webhook event representing an action done with elevated security access"
)
public class ElevatedAccessEvent implements Comparable<ElevatedAccessEvent> {
  private final String user;
  private final String requestId;
  private final SingularityAuthorizationScope scope;
  private final long createdAt;

  @JsonCreator
  public ElevatedAccessEvent(
    @JsonProperty("user") String user,
    @JsonProperty("requestId") String requestId,
    @JsonProperty("scope") SingularityAuthorizationScope scope,
    @JsonProperty("createdAt") long createdAt
  ) {
    this.user = user;
    this.requestId = requestId;
    this.scope = scope;
    this.createdAt = createdAt;
  }

  @Schema(description = "The user id for the jita event")
  public String getUser() {
    return user;
  }

  @Schema(description = "The request Id in Singularity")
  public String getRequestId() {
    return requestId;
  }

  @Schema(description = "The scope used in the jita event")
  public SingularityAuthorizationScope getScope() {
    return scope;
  }

  @Schema(description = "The time the jita was used")
  public long getCreatedAt() {
    return createdAt;
  }

  @Override
  public int compareTo(ElevatedAccessEvent o) {
    return ComparisonChain
      .start()
      .compare(o.getCreatedAt(), createdAt)
      .compare(o.getScope(), scope)
      .compare(o.getUser(), user)
      .compare(o.getRequestId(), requestId)
      .result();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ElevatedAccessEvent that = (ElevatedAccessEvent) o;
    return (
      createdAt == that.createdAt &&
      Objects.equals(user, that.user) &&
      Objects.equals(requestId, that.requestId) &&
      scope == that.scope
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(user, requestId, scope, createdAt);
  }

  @Override
  public String toString() {
    return (
      "ElevatedAccessEvent{" +
      "user='" +
      user +
      '\'' +
      ", requestId='" +
      requestId +
      '\'' +
      ", scope=" +
      scope +
      ", createdAt=" +
      createdAt +
      '}'
    );
  }
}
