package com.hubspot.singularity.api.deploy.mesos;

import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.Beta;

import io.swagger.v3.oas.annotations.media.Schema;

@Beta
@Schema(description = "Represents a docker port mapping")
public class SingularityPortMapping {
  private final int hostPort, containerPort;
  private final Optional<String> protocol;

  @JsonCreator
  public SingularityPortMapping(@JsonProperty("hostPort") int hostPort,
      @JsonProperty("containerPort") int containerPort,
      @JsonProperty("protocol") Optional<String> protocol) {
    this.hostPort = hostPort;
    this.containerPort = containerPort;
    this.protocol = protocol;
  }

  @Schema(required = true, description = "the port to map from on the host network interface")
  public int getHostPort() {
    return hostPort;
  }

  @Schema(required = true, description = "the port to map to on the container network interface")
  public int getContainerPort() {
    return containerPort;
  }

  @Schema(description = "the protocol e.g. 'tcp' or 'udp'")
  public Optional<String> getProtocol() {
    return protocol;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityPortMapping that = (SingularityPortMapping) o;
    return hostPort == that.hostPort &&
        this.containerPort == that.containerPort &&
        Objects.equals(protocol, that.protocol);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hostPort, containerPort, protocol);
  }

  @Override
  public String toString() {
    return "SingularityPortMapping{" +
        "hostPort='" + hostPort + '\'' +
        ", containerPort=" + containerPort +
        ", protocol=" + protocol +
        '}';
  }
}
