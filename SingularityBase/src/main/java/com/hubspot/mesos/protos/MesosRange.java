package com.hubspot.mesos.protos;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class MesosRange {
  private final Optional<Long> begin;
  private final Optional<Long> end;

  @JsonCreator
  public MesosRange(@JsonProperty("being") Optional<Long> begin, @JsonProperty("end") Optional<Long> end) {
    this.begin = begin;
    this.end = end;
  }

  public Long getBegin() {
    return begin.orNull();
  }

  @JsonIgnore
  public boolean hasBegin() {
    return begin.isPresent();
  }

  public Long getEnd() {
    return end.orNull();
  }

  @JsonIgnore
  public boolean hasEnd() {
    return end.isPresent();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof MesosRange) {
      final MesosRange that = (MesosRange) obj;
      return Objects.equals(this.begin, that.begin) &&
          Objects.equals(this.end, that.end);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(begin, end);
  }

  @Override
  public String toString() {
    return "SingularityMesosRange{" +
        "begin=" + begin +
        ", end=" + end +
        '}';
  }
}
