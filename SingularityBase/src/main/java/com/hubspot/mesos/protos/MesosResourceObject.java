package com.hubspot.mesos.protos;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class MesosResourceObject {
  private final Optional<String> name;
  private final Optional<MesosRangesObject> ranges;
  private final Map<String, Object> allOtherFields;

  @JsonCreator

  public MesosResourceObject(@JsonProperty("name") Optional<String> name,
                             @JsonProperty("ranges") Optional<MesosRangesObject> ranges) {
    this.name = name;
    this.ranges = ranges;
    this.allOtherFields = new HashMap<>();
  }

  public String getName() {
    return name.orNull();
  }

  @JsonIgnore
  public boolean hasName() {
    return name.isPresent();
  }

  public MesosRangesObject getRanges() {
    return ranges.orNull();
  }

  @JsonIgnore
  public boolean hasRanges() {
    return ranges.isPresent();
  }

  // Unknown fields
  @JsonAnyGetter
  public Map<String, Object> getAllOtherFields() {
    return allOtherFields;
  }

  @JsonAnySetter
  public void setAllOtherFields(String name, Object value) {
    allOtherFields.put(name, value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof MesosResourceObject) {
      final MesosResourceObject that = (MesosResourceObject) obj;
      return Objects.equals(this.name, that.name) &&
          Objects.equals(this.ranges, that.ranges) &&
          Objects.equals(this.allOtherFields, that.allOtherFields);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, ranges, allOtherFields);
  }

  @Override
  public String toString() {
    return "MesosResourceObject{" +
        "name=" + name +
        ", ranges=" + ranges +
        ", allOtherFields=" + allOtherFields +
        '}';
  }
}
