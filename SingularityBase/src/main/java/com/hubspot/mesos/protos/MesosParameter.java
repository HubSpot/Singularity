package com.hubspot.mesos.protos;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class MesosParameter {
  private final Optional<String> key;
  private final Optional<String> value;

  @JsonCreator
  public MesosParameter(@JsonProperty("key") Optional<String> key,
                        @JsonProperty("value") Optional<String> value) {
    this.key = key;
    this.value = value;
  }

  public String getKey() {
    return key.orNull();
  }

  @JsonIgnore
  public boolean hasKey() {
    return key.isPresent();
  }

  public String getValue() {
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
    if (obj instanceof MesosParameter) {
      final MesosParameter that = (MesosParameter) obj;
      return Objects.equals(this.key, that.key) &&
          Objects.equals(this.value, that.value);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value);
  }

  @Override
  public String toString() {
    return "MesosKeyValueObject{" +
        "key=" + key +
        ", value=" + value +
        '}';
  }
}
