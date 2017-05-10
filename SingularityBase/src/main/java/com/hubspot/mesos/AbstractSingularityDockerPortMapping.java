package com.hubspot.mesos;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Immutable
@SingularityStyle
@JsonDeserialize(as = SingularityDockerPortMapping.class)
public abstract class AbstractSingularityDockerPortMapping {
  @Default
  @ApiModelProperty(required = false, value = "Container port. Use the port number provided (LITERAL) or the dynamically allocated port at this index (FROM_OFFER)")
  public SingularityPortMappingType getContainerPortType() {
    return SingularityPortMappingType.LITERAL;
  }

  @ApiModelProperty(required = true, value = "Port number, or index of port from offer within the container")
  public abstract int getContainerPort();

  @Default
  @ApiModelProperty(required = false, value = "Host port. Use the port number provided (LITERAL) or the dynamically allocated port at this index (FROM_OFFER)")
  public SingularityPortMappingType getHostPortType() {
    return SingularityPortMappingType.LITERAL;
  }

  @ApiModelProperty(required = true, value = "Port number, or index of port from offer on the host")
  public abstract int getHostPort();

  @Default
  @ApiModelProperty(required = false, value = "Protocol for binding the port. Default is tcp")
  public String getProtocol() {
    return "tcp";
  }

  @Deprecated
  public static SingularityDockerPortMapping of(Optional<SingularityPortMappingType> containerPortType,
                                                int containerPort,
                                                Optional<SingularityPortMappingType> hostPortType,
                                                int hostPort,
                                                Optional<String> protocol) {
    return SingularityDockerPortMapping.builder()
        .setContainerPortType(containerPortType.or(SingularityPortMappingType.LITERAL))
        .setContainerPort(containerPort)
        .setHostPortType(hostPortType.or(SingularityPortMappingType.LITERAL))
        .setHostPort(hostPort)
        .setProtocol(protocol.or("tcp"))
        .build();
  }
}
