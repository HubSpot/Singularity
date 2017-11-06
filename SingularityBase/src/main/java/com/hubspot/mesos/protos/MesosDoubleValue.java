package com.hubspot.mesos.protos;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class MesosDoubleValue {
  private final Optional<Double> value;

  @JsonCreator
  public MesosDoubleValue(@JsonProperty("value") Optional<Double> value) {
    this.value = value;
  }

  public Double getValue() {
    return value.orNull();
  }

  @JsonIgnore
  public boolean hasValue() {
    return value.isPresent();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof MesosDoubleValue) {
      final MesosDoubleValue that = (MesosDoubleValue) obj;
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
    return "MesosDoubleValue{" +
        "value=" + value +
        '}';
  }
}
