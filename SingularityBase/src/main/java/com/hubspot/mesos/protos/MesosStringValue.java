package com.hubspot.mesos.protos;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MesosStringValue {
  private final String value;

  @JsonCreator
  public MesosStringValue(@JsonProperty("value") String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof MesosStringValue) {
      final MesosStringValue that = (MesosStringValue) obj;
      return Objects.equals(this.value, that.value);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return "SingularityMesosIdObject{" +
        "value='" + value + '\'' +
        '}';
  }
}
