package com.hubspot.mesos.protos;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class MesosRangesObject {
  private final Optional<List<MesosRangeObject>> range;

  @JsonCreator
  public MesosRangesObject(@JsonProperty("range") Optional<List<MesosRangeObject>> range) {
    this.range = range;
  }

  public List<MesosRangeObject> getRange() {
    return range.or(Collections.emptyList());
  }

  @JsonIgnore // to mimic mesos
  public List<MesosRangeObject> getRangesList() {
    return getRange();
  }

  @JsonIgnore
  public boolean hasRange() {
    return range.isPresent();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof MesosRangesObject) {
      final MesosRangesObject that = (MesosRangesObject) obj;
      return Objects.equals(this.range, that.range);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(range);
  }

  @Override
  public String toString() {
    return "MesosRangesObject{" +
        "range=" + range +
        '}';
  }
}
