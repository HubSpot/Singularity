package com.hubspot.singularity.api;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityAction;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Notes related to a particular disabled action")
public class SingularityDisabledActionRequest {
  private final SingularityAction type;
  private final Optional<String> message;

  @JsonCreator
  public SingularityDisabledActionRequest(@JsonProperty("type") SingularityAction type, @JsonProperty("message") Optional<String> message) {
    this.type = type;
    this.message = message;
  }

  @Schema(required = true, description = "The type of action to disable")
  public SingularityAction getType() {
    return type;
  }

  @Schema(description = "An optional message/reason for disabling the action specified")
  public Optional<String> getMessage() {
    return message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityDisabledActionRequest that = (SingularityDisabledActionRequest) o;
    return type == that.type &&
        Objects.equals(message, that.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, message);
  }

  @Override
  public String toString() {
    return "SingularityDisabledActionRequest{" +
        "type=" + type +
        ", message=" + message +
        '}';
  }
}
