package com.hubspot.mesos.protos;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class MesosDiskPersistence {
  private final Optional<String> id;
  private final Optional<String> principal;

  @JsonCreator

  public MesosDiskPersistence(@JsonProperty("id") Optional<String> id,
                              @JsonProperty("principal") Optional<String> principal) {
    this.id = id;
    this.principal = principal;
  }

  public String getId() {
    return id.orNull();
  }

  @JsonIgnore
  public boolean hasId() {
    return id.isPresent();
  }

  public String getPrincipal() {
    return principal.orNull();
  }

  @JsonIgnore
  public boolean hasPrincipal() {
    return this.principal.isPresent();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof MesosDiskPersistence) {
      final MesosDiskPersistence that = (MesosDiskPersistence) obj;
      return Objects.equals(this.id, that.id) &&
          Objects.equals(this.principal, that.principal);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, principal);
  }

  @Override
  public String toString() {
    return "MesosDiskPersistence{" +
        "id=" + id +
        ", principal=" + principal +
        '}';
  }
}
