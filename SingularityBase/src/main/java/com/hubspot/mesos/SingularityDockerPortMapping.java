package com.hubspot.mesos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.ApiModelProperty;

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
    this.containerPortType = containerPortType.or(DEFAULT_PORT_MAPPING_TYPE);
    this.containerPort = containerPort;
    this.hostPortType = hostPortType.or(DEFAULT_PORT_MAPPING_TYPE);
    this.hostPort = hostPort;
    this.protocol = protocol.or(DEFAULT_PROTOCOL);
  }

  @ApiModelProperty(required=false, value="Container port. Use the port number provided (LITERAL) or the dynamically allocated port at this index (FROM_OFFER)")
  public SingularityPortMappingType getContainerPortType() {
    return containerPortType;
  }

  @ApiModelProperty(required=true, value="Port number, or index of port from offer within the container")
  public int getContainerPort() {
    return containerPort;
  }

  @ApiModelProperty(required=false, value="Host port. Use the port number provided (LITERAL) or the dynamically allocated port at this index (FROM_OFFER)")
  public SingularityPortMappingType getHostPortType() {
    return hostPortType;
  }

  @ApiModelProperty(required=true, value="Port number, or index of port from offer on the host")
  public int getHostPort() {
    return hostPort;
  }

  @ApiModelProperty(required=false, value="Protocol for binding the port. Default is tcp")
  public String getProtocol() {
    return protocol;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("containerPortType", containerPortType)
      .add("hostPortType", hostPortType)
      .add("containerPort", containerPort)
      .add("hostPort", hostPort)
      .add("protocol", protocol)
      .toString();
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

    if (!containerPortType.equals(that.containerPortType)) {
      return false;
    }
    if (containerPort != that.containerPort) {
      return false;
    }
    if (!hostPortType.equals(that.hostPortType)) {
      return false;
    }
    if (!protocol.equals(that.protocol)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = containerPortType.hashCode();
    result = 31 * result + hostPortType.hashCode();
    result = 31 * result + containerPort;
    result = 31 * result + hostPort;
    result = 31 * result + protocol.hashCode();
    return result;
  }
}
