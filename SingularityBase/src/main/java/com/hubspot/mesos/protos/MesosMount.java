package com.hubspot.mesos.protos;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class MesosMount {
  private final Optional<String> root;

  @JsonCreator
  public MesosMount(@JsonProperty("root") Optional<String> root) {
    this.root = root;
  }

  public Optional<String> getRoot() {
    return root;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof MesosMount) {
      final MesosMount that = (MesosMount) obj;
      return Objects.equals(this.root, that.root);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(root);
  }

  @Override
  public String toString() {
    return "MesosMount{" +
        "root=" + root +
        '}';
  }
}
