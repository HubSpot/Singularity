package com.hubspot.mesos.protos;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class MesosDiskSource {
  private final Optional<MesosMount> mount;
  private final Optional<MesosMount> path;
  private final Optional<MesosDiskSourceType> type;

  @JsonCreator
  public MesosDiskSource(@JsonProperty("mount") Optional<MesosMount> mount,
                         @JsonProperty("path") Optional<MesosMount> path,
                         @JsonProperty("type") Optional<MesosDiskSourceType> type) {
    this.mount = mount;
    this.path = path;
    this.type = type;
  }

  public MesosMount getMount() {
    return mount.orNull();
  }

  public boolean hasMount() {
    return mount.isPresent();
  }

  public MesosMount getPath() {
    return path.orNull();
  }

  public boolean hasPath() {
    return path.isPresent();
  }

  public MesosDiskSourceType getType() {
    return type.orNull();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof MesosDiskSource) {
      final MesosDiskSource that = (MesosDiskSource) obj;
      return Objects.equals(this.mount, that.mount) &&
          Objects.equals(this.path, that.path) &&
          Objects.equals(this.type, that.type);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(mount, path, type);
  }

  @Override
  public String toString() {
    return "MesosDiskSource{" +
        "mount=" + mount +
        ", path=" + path +
        ", type=" + type +
        '}';
  }
}
