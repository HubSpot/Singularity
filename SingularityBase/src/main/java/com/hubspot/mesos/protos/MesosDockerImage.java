package com.hubspot.mesos.protos;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class MesosDockerImage {
  private final Optional<String> name;
  private final Optional<MesosCredential> credential;

  @JsonCreator
  public MesosDockerImage(@JsonProperty("name") Optional<String> name,
                          @JsonProperty("credential") Optional<MesosCredential> credential) {
    this.name = name;
    this.credential = credential;
  }

  public String getName() {
    return name.orNull();
  }

  public boolean hasName() {
    return name.isPresent();
  }

  public MesosCredential getCredential() {
    return credential.orNull();
  }

  public boolean hasCredential() {
    return credential.isPresent();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof MesosDockerImage) {
      final MesosDockerImage that = (MesosDockerImage) obj;
      return Objects.equals(this.name, that.name) &&
          Objects.equals(this.credential, that.credential);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, credential);
  }

  @Override
  public String toString() {
    return "MesosDockerImage{" +
        "name=" + name +
        ", credential=" + credential +
        '}';
  }
}
