package com.hubspot.mesos.protos;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MesosRanges {
  private final List<MesosRange> range;

  @JsonCreator
  public MesosRanges(@JsonProperty("range") List<MesosRange> range) {
    this.range = range;
  }

  public List<MesosRange> getRange() {
    return range;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof MesosRanges) {
      final MesosRanges that = (MesosRanges) obj;
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
    return "SingularityMesosRanges{" +
        "range=" + range +
        '}';
  }
}
