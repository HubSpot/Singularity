package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class SingularityDisaster {
  private final SingularityDisasterType type;
  private final boolean active;

  @JsonCreator
  public SingularityDisaster(@JsonProperty("type") SingularityDisasterType type, @JsonProperty("boolean") boolean active) {
    this.type = type;
    this.active = active;
  }

  public SingularityDisasterType getType() {
    return type;
  }

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
    return Objects.hashCode(type, active);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("type", type)
      .add("active", active)
      .toString();
  }
}
