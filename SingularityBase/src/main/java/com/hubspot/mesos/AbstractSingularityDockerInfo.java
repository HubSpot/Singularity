package com.hubspot.mesos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Immutable
@SingularityStyle
public abstract class AbstractSingularityDockerInfo {
  @ApiModelProperty(required = true, value = "Docker image name")
  public abstract String getImage();

  @ApiModelProperty(required = true, value = "Controls use of the docker --privleged flag")
  public abstract boolean isPrivileged();

  @ApiModelProperty(required = false, value = "Docker network type. Value can be BRIDGE, HOST, or NONE", dataType = "com.hubspot.mesos.SingularityDockerNetworkType")
  public abstract Optional<SingularityDockerNetworkType> getNetwork();

  @ApiModelProperty(required = false, value = "List of port mappings")
  public abstract List<SingularityDockerPortMapping> getPortMappings();

  @Default
  @ApiModelProperty(required = false, value = "Always run docker pull even if the image already exists locally")
  public boolean isForcePullImage() {
    return false;
  }

  @Deprecated
  public abstract Optional<Map<String, String>> getParameters();

  @ApiModelProperty(required = false, value = "Other docker run command line options to be set")
  public abstract List<SingularityDockerParameter> getDockerParameters();

  @JsonIgnore
  public boolean hasAllLiteralHostPortMappings() {
    for (SingularityDockerPortMapping mapping : getPortMappings()) {
      if (mapping.getHostPortType() == SingularityPortMappingType.FROM_OFFER) {
        return false;
      }
    }
    return true;
  }

  @JsonIgnore
  public List<Long> getLiteralHostPorts() {
    List<Long> literalHostPorts = new ArrayList<>();
    for (SingularityDockerPortMapping mapping : getPortMappings()) {
      if (mapping.getHostPortType() == SingularityPortMappingType.LITERAL) {
        long port = mapping.getHostPort();
        literalHostPorts.add(port);
      }
    }
    return literalHostPorts;
  }
}
