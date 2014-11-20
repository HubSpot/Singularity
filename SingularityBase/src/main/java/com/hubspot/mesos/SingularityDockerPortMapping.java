package com.hubspot.mesos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

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

  public SingularityPortMappingType getContainerPortType() {
    return containerPortType;
  }

  public int getContainerPort() {
    return containerPort;
  }

  public SingularityPortMappingType getHostPortType() {
    return hostPortType;
  }

  public int getHostPort() {
    return hostPort;
  }

  public String getProtocol() {
    return protocol;
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
