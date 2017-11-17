package com.hubspot.mesos.protos;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MesosLabels {
  private final List<MesosParameter> labels;

  @JsonCreator
  public MesosLabels(@JsonProperty("labels") List<MesosParameter> labels) {
    this.labels = labels;
  }

  public List<MesosParameter> getLabels() {
    return labels;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof MesosLabels) {
      final MesosLabels that = (MesosLabels) obj;
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
    return "MesosLabels{" +
        "labels=" + labels +
        '}';
  }
}
