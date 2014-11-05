package com.hubspot.mesos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityDockerPortMapping {
  public static final String DEFAULT_PROTOCOL = "tcp";
  public static final PortMappingType DEFAULT_PORT_MAPPING_TYPE = PortMappingType.LITERAL;

  private final Optional<PortMappingType> containerPortType;
  private final Optional<PortMappingType> hostPortType;
  private final int containerPort;
  private final int hostPort;
  private final Optional<String> protocol;

  @JsonCreator
  public SingularityDockerPortMapping(@JsonProperty("containerPortType") Optional<PortMappingType> containerPortType,
                                      @JsonProperty("containerPort") int containerPort,
                                      @JsonProperty("hostPortType") Optional<PortMappingType> hostPortType,
                                      @JsonProperty("hostPort") int hostPort,
                                      @JsonProperty("protocol") Optional<String> protocol) {
    this.containerPortType = containerPortType;
    this.containerPort = containerPort;
    this.hostPortType = hostPortType;
    this.hostPort = hostPort;
    this.protocol = protocol;
  }

  public Optional<PortMappingType> getContainerPortType() {
    return containerPortType;
  }

  public int getContainerPort() {
    return containerPort;
  }

  public Optional<PortMappingType> getHostPortType() {
    return hostPortType;
  }

  public int getHostPort() {
    return hostPort;
  }

  public Optional<String> getProtocol() {
    return protocol;
  }

  public static enum PortMappingType {
    LITERAL,
    FROM_OFFER
  }

  @Override
  public String toString() {
    return "SingularityDockerPortMapping[" +
            "containerPortType=" + containerPortType +
            ", hostPortType=" + hostPortType +
            ", containerPort=" + containerPort +
            ", hostPort=" + hostPort +
            ", protocol=" + protocol +
            ']';
  }
}
