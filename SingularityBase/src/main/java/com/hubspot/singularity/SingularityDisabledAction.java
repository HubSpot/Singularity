package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
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
      Objects.equal(message, that.message) &&
      Objects.equal(user, that.user) &&
      Objects.equal(expiresAt, that.expiresAt);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(type, message, user, automaticallyClearable, expiresAt);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("type", type)
      .add("message", message)
      .add("user", user)
      .add("automaticallyClearable", automaticallyClearable)
      .add("expiresAt", expiresAt)
      .toString();
  }
}
