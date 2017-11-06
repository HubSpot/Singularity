package com.hubspot.mesos.protos;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class MesosAppcImage {
  private final Optional<String> id;
  private final Optional<String> name;
  private final Optional<List<MesosParameter>> labels;

  @JsonCreator

  public MesosAppcImage(@JsonProperty("id") Optional<String> id,
                        @JsonProperty("name") Optional<String> name,
                        @JsonProperty("labels") Optional<List<MesosParameter>> labels) {
    this.id = id;
    this.name = name;
    this.labels = labels;
  }

  public String getId() {
    return id.orNull();
  }

  public boolean hasId() {
    return id.isPresent();
  }

  public String getName() {
    return name.orNull();
  }

  public boolean hasName() {
    return name.isPresent();
  }

  public List<MesosParameter> getLabels() {
    return labels.orNull();
  }

  public boolean hasLabels() {
    return labels.isPresent();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof MesosAppcImage) {
      final MesosAppcImage that = (MesosAppcImage) obj;
      return Objects.equals(this.id, that.id) &&
          Objects.equals(this.name, that.name) &&
          Objects.equals(this.labels, that.labels);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, labels);
  }

  @Override
  public String toString() {
    return "MesosAppcImage{" +
        "id=" + id +
        ", name=" + name +
        ", labels=" + labels +
        '}';
  }
}
