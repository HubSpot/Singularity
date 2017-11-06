package com.hubspot.mesos.protos;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class MesosAttributeObject {
  private final Optional<String> name;
  private final Optional<MesosAttributeType> type;
  private final Optional<MesosStringValue> text;
  private final Optional<MesosDoubleValue> scalar;
  private final Optional<MesosRanges> ranges;
  private final Optional<MesosSet> set;

  @JsonCreator

  public MesosAttributeObject(@JsonProperty("name") Optional<String> name,
                              @JsonProperty("type") Optional<MesosAttributeType> type,
                              @JsonProperty("text") Optional<MesosStringValue> text,
                              @JsonProperty("scalar") Optional<MesosDoubleValue> scalar,
                              @JsonProperty("ranges") Optional<MesosRanges> ranges,
                              @JsonProperty("set") Optional<MesosSet> set) {
    this.name = name;
    this.type = type;
    this.text = text;
    this.scalar = scalar;
    this.ranges = ranges;
    this.set = set;
  }

  public String getName() {
    return name.orNull();
  }

  @JsonIgnore
  public boolean hasName() {
    return name.isPresent();
  }

  public MesosAttributeType getType() {
    return type.orNull();
  }

  @JsonIgnore
  public boolean hasType() {
    return type.isPresent();
  }

  public MesosStringValue getText() {
    return text.orNull();
  }

  @JsonIgnore
  public boolean hasText() {
    return text.isPresent();
  }

  public MesosDoubleValue getScalar() {
    return scalar.orNull();
  }

  @JsonIgnore
  public boolean hasScalar() {
    return scalar.isPresent();
  }

  public MesosRanges getRanges() {
    return ranges.orNull();
  }

  @JsonIgnore
  public boolean hasRanges() {
    return ranges.isPresent();
  }

  public MesosSet getSet() {
    return set.orNull();
  }

  @JsonIgnore
  public boolean hasSet() {
    return set.isPresent();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof MesosAttributeObject) {
      final MesosAttributeObject that = (MesosAttributeObject) obj;
      return Objects.equals(this.name, that.name) &&
          Objects.equals(this.type, that.type) &&
          Objects.equals(this.text, that.text) &&
          Objects.equals(this.scalar, that.scalar) &&
          Objects.equals(this.ranges, that.ranges) &&
          Objects.equals(this.set, that.set);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, text, scalar, ranges, set);
  }

  @Override
  public String toString() {
    return "SingularityMesosAttributeObject{" +
        "name='" + name + '\'' +
        ", type=" + type +
        ", text=" + text +
        ", scalar=" + scalar +
        ", ranges=" + ranges +
        ", set=" + set +
        '}';
  }
}
