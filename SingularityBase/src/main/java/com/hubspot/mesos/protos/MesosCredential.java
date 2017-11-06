package com.hubspot.mesos.protos;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class MesosCredential {
  private final Optional<String> principal;
  private final Optional<String> secret;

  @JsonCreator
  public MesosCredential(@JsonProperty("principal") Optional<String> principal,
                         @JsonProperty("secret") Optional<String> secret) {
    this.principal = principal;
    this.secret = secret;
  }

  public String getPrincipal() {
    return principal.orNull();
  }

  public boolean hasPrincipal() {
    return principal.isPresent();
  }

  public String getSecret() {
    return secret.orNull();
  }

  public boolean hasSecret() {
    return secret.isPresent();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof MesosCredential) {
      final MesosCredential that = (MesosCredential) obj;
      return Objects.equals(this.principal, that.principal) &&
          Objects.equals(this.secret, that.secret);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(principal, secret);
  }

  @Override
  public String toString() {
    return "MesosCredential{" +
        "principal=" + principal +
        ", secret=" + secret +
        '}';
  }
}
