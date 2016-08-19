package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

public class SingularityDisabledAction {
  private final SingularityDisabledActionType type;
  private final String message;
  private final Optional<String> user;

  @JsonCreator
  public SingularityDisabledAction(@JsonProperty("type") SingularityDisabledActionType type, @JsonProperty("message") String message, @JsonProperty("user") Optional<String> user) {
    this.type = type;
    this.message = message;
    this.user = user;
  }

  public SingularityDisabledActionType getType() {
    return type;
  }

  public String getMessage() {
    return message;
  }

  public Optional<String> getUser() {
    return user;
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
    return type == that.type &&
      Objects.equal(message, that.message) &&
      Objects.equal(user, that.user);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(type, message, user);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("type", type)
      .add("message", message)
      .add("user", user)
      .toString();
  }
}
