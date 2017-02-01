package com.hubspot.singularity.athena;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AthenaPartitionWithValue {
  private final AthenaPartitionType type;
  private final String value;

  @JsonCreator

  public AthenaPartitionWithValue(@JsonProperty("type") AthenaPartitionType type,
                                  @JsonProperty("value") String value) {
    this.type = type;
    this.value = value;
  }

  public AthenaPartitionType getType() {
    return type;
  }

  public String getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AthenaPartitionWithValue that = (AthenaPartitionWithValue) o;

    if (type != that.type) {
      return false;
    }
    return value != null ? value.equals(that.value) : that.value == null;
  }

  @Override
  public int hashCode() {
    int result = type != null ? type.hashCode() : 0;
    result = 31 * result + (value != null ? value.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "AthenaPartitionWithValue{" +
        "type=" + type +
        ", value='" + value + '\'' +
        '}';
  }
}
