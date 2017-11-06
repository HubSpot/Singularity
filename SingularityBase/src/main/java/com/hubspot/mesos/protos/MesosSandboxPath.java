package com.hubspot.mesos.protos;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class MesosSandboxPath {
  private final Optional<String> path;
  private final Optional<MesosSandboxPathType> type;

  @JsonCreator

  public MesosSandboxPath(@JsonProperty("path") Optional<String> path,
                          @JsonProperty("type") Optional<MesosSandboxPathType> type) {
    this.path = path;
    this.type = type;
  }

  public String getPath() {
    return path.orNull();
  }

  public boolean hasPath() {
    return path.isPresent();
  }

  public MesosSandboxPathType getType() {
    return type.orNull();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof MesosSandboxPath) {
      final MesosSandboxPath that = (MesosSandboxPath) obj;
      return Objects.equals(this.path, that.path) &&
          Objects.equals(this.type, that.type);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, type);
  }

  @Override
  public String toString() {
    return "MesosSandboxPath{" +
        "path=" + path +
        ", type=" + type +
        '}';
  }
}
