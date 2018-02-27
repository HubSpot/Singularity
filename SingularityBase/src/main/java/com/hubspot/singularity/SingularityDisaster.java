package com.hubspot.singularity;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representation of an active disaster")
public class SingularityDisaster {
  private final SingularityDisasterType type;
  private final boolean active;

  @JsonCreator
  public SingularityDisaster(@JsonProperty("type") SingularityDisasterType type, @JsonProperty("boolean") boolean active) {
    this.type = type;
    this.active = active;
  }

  @Schema(description = "The enum type of disaster, indicative of cause")
  public SingularityDisasterType getType() {
    return type;
  }

  @Schema(description = "`true` if a disaster of this type is currently active")
  public boolean isActive() {
    return active;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityDisaster that = (SingularityDisaster) o;
    return active == that.active &&
        type == that.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, active);
  }

  @Override
  public String toString() {
    return "SingularityDisaster{" +
        "type=" + type +
        ", active=" + active +
        '}';
  }
}
