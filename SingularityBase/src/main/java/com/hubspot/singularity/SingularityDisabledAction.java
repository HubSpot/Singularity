package com.hubspot.singularity;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityDisabledAction {
  private final SingularityAction type;
  private final String message;
  private final Optional<String> user;
  private final boolean automaticallyClearable;
  private final Optional<Long> expiresAt;

  @JsonCreator
  public SingularityDisabledAction(@JsonProperty("type") SingularityAction type, @JsonProperty("message") String message, @JsonProperty("user") Optional<String> user,
                                   @JsonProperty("automaticallyClearable") boolean automaticallyClearable, @JsonProperty("expiresAt") Optional<Long> expiresAt) {
    this.type = type;
    this.message = message;
    this.user = user;
    this.automaticallyClearable = automaticallyClearable;
    this.expiresAt = expiresAt;
  }

  public SingularityAction getType() {
    return type;
  }

  public String getMessage() {
    return message;
  }

  public Optional<String> getUser() {
    return user;
  }

  public boolean isAutomaticallyClearable() {
    return automaticallyClearable;
  }

  public Optional<Long> getExpiresAt() {
    return expiresAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityDisabledAction that = (SingularityDisabledAction) o;
    return automaticallyClearable == that.automaticallyClearable &&
        type == that.type &&
        Objects.equals(message, that.message) &&
        Objects.equals(user, that.user) &&
        Objects.equals(expiresAt, that.expiresAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, message, user, automaticallyClearable, expiresAt);
  }

  @Override
  public String toString() {
    return "SingularityDisabledAction{" +
        "type=" + type +
        ", message='" + message + '\'' +
        ", user=" + user +
        ", automaticallyClearable=" + automaticallyClearable +
        ", expiresAt=" + expiresAt +
        '}';
  }
}
