package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityAction;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityDisabledActionRequest {
  private final SingularityAction type;
  private final Optional<String> message;

  @JsonCreator
  public SingularityDisabledActionRequest(@JsonProperty("type") SingularityAction type, @JsonProperty("message") Optional<String> message) {
    this.type = type;
    this.message = message;
  }

  @ApiModelProperty(required=true, value="The type of action to disable")
  public SingularityAction getType() {
    return type;
  }

  @ApiModelProperty(required=false, value="An optional message/reason for disabling the action specified")
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
      Objects.equal(message, that.message);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(type, message);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("type", type)
      .add("message", message)
      .toString();
  }
}
