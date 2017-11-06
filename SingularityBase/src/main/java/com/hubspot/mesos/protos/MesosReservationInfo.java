package com.hubspot.mesos.protos;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Optional;

public class MesosReservationInfo {
  private final Optional<List<MesosParameter>> labels;

  @JsonCreator
  public MesosReservationInfo(Optional<List<MesosParameter>> labels) {
    this.labels = labels;
  }

  public List<MesosParameter> getLabels() {
    return labels.orNull();
  }

  @JsonIgnore
  public boolean hasLabels() {
    return labels.isPresent();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof MesosReservationInfo) {
      final MesosReservationInfo that = (MesosReservationInfo) obj;
      return Objects.equals(this.labels, that.labels);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(labels);
  }

  @Override
  public String toString() {
    return "MesosReservationInfo{" +
        "labels=" + labels +
        '}';
  }
}
