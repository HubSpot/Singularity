package com.hubspot.mesos.protos;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MesosSet {
  private final List<String> item;

  @JsonCreator
  public MesosSet(@JsonProperty("item") List<String> item) {
    this.item = item;
  }

  public List<String> getItem() {
    return item;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof MesosSet) {
      final MesosSet that = (MesosSet) obj;
      return Objects.equals(this.item, that.item);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(item);
  }

  @Override
  public String toString() {
    return "SingularityMesosSet{" +
        "item=" + item +
        '}';
  }
}
