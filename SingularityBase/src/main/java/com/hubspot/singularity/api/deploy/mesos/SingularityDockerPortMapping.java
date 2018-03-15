package com.hubspot.singularity.api.deploy.mesos;

import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Describes a docker port mapping")
public class SingularityDockerPortMapping {
  public static final String DEFAULT_PROTOCOL = "tcp";
  public static final SingularityPortMappingType DEFAULT_PORT_MAPPING_TYPE = SingularityPortMappingType.LITERAL;

  private final SingularityPortMappingType containerPortType;
  private final SingularityPortMappingType hostPortType;
  private final int containerPort;
  private final int hostPort;
  private final String protocol;

  @JsonCreator
  public SingularityDockerPortMapping(@JsonProperty("containerPortType") Optional<SingularityPortMappingType> containerPortType,
                                      @JsonProperty("containerPort") int containerPort,
                                      @JsonProperty("hostPortType") Optional<SingularityPortMappingType> hostPortType,
                                      @JsonProperty("hostPort") int hostPort,
                                      @JsonProperty("protocol") Optional<String> protocol) {
    this.containerPortType = containerPortType.orElse(DEFAULT_PORT_MAPPING_TYPE);
    this.containerPort = containerPort;
    this.hostPortType = hostPortType.orElse(DEFAULT_PORT_MAPPING_TYPE);
    this.hostPort = hostPort;
    this.protocol = protocol.orElse(DEFAULT_PROTOCOL);
  }

  @Schema(description = "Container port. Use the port number provided (LITERAL) or the dynamically allocated port at this index (FROM_OFFER)")
  public SingularityPortMappingType getContainerPortType() {
    return containerPortType;
  }

  @Schema(required = true, description = "Port number, or index of port from offer within the container")
  public int getContainerPort() {
    return containerPort;
  }

  @Schema(description = "Host port. Use the port number provided (LITERAL) or the dynamically allocated port at this index (FROM_OFFER)")
  public SingularityPortMappingType getHostPortType() {
    return hostPortType;
  }

  @Schema(required = true, description = "Port number, or index of port from offer on the host")
  public int getHostPort() {
    return hostPort;
  }

  @Schema(description = "Protocol for binding the port. Default is tcp")
  public String getProtocol() {
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
    SingularityDockerPortMapping that = (SingularityDockerPortMapping) o;
    return containerPort == that.containerPort &&
        hostPort == that.hostPort &&
        containerPortType == that.containerPortType &&
        hostPortType == that.hostPortType &&
        Objects.equals(protocol, that.protocol);
  }

  @Override
  public int hashCode() {
    return Objects.hash(containerPortType, hostPortType, containerPort, hostPort, protocol);
  }

  @Override
  public String toString() {
    return "SingularityDockerPortMapping{" +
        "containerPortType=" + containerPortType +
        ", hostPortType=" + hostPortType +
        ", containerPort=" + containerPort +
        ", hostPort=" + hostPort +
        ", protocol='" + protocol + '\'' +
        '}';
  }
}
