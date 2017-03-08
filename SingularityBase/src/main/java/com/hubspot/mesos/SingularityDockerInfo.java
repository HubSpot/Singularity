package com.hubspot.mesos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityDockerInfo {
  private final String image;
  private final boolean privileged;
  private final Optional<SingularityDockerNetworkType> network;
  private final List<SingularityDockerPortMapping> portMappings;
  private final boolean forcePullImage;
  private final Optional<Map<String, String>> parameters;
  private final List<SingularityDockerParameter> dockerParameters;

  @JsonCreator
  public SingularityDockerInfo(@JsonProperty("image") String image,
                               @JsonProperty("privileged") boolean privileged,
                               @JsonProperty("network") SingularityDockerNetworkType network,
                               @JsonProperty("portMappings") Optional<List<SingularityDockerPortMapping>> portMappings,
                               @JsonProperty("forcePullImage") Optional<Boolean> forcePullImage,
                               @JsonProperty("parameters") Optional<Map<String, String>> parameters,
                               @JsonProperty("dockerParameters") Optional<List<SingularityDockerParameter>> dockerParameters) {
    this.image = image;
    this.privileged = privileged;
    this.network = Optional.fromNullable(network);
    this.portMappings = portMappings.or(Collections.<SingularityDockerPortMapping>emptyList());
    this.forcePullImage = forcePullImage.or(false);
    this.parameters = parameters;
    this.dockerParameters = dockerParameters.or(parameters.isPresent() ? SingularityDockerParameter.parametersFromMap(parameters.get()) : Collections.<SingularityDockerParameter>emptyList());
  }

  public SingularityDockerInfo(String image, boolean privileged, SingularityDockerNetworkType network, Optional<List<SingularityDockerPortMapping>> portMappings, Optional<Boolean> forcePullImage, List<SingularityDockerParameter> dockerParameters) {
    this(image, privileged, network, portMappings, forcePullImage, Optional.<Map<String,String>>absent(), Optional.of(dockerParameters));
  }

  @Deprecated
  public SingularityDockerInfo(String image, boolean privileged, SingularityDockerNetworkType network, Optional<List<SingularityDockerPortMapping>> portMappings, Optional<Boolean> forcePullImage, Optional<Map<String, String>> parameters) {
    this(image, privileged, network, portMappings, forcePullImage, parameters, Optional.<List<SingularityDockerParameter>>absent());
  }

  @Deprecated
  public SingularityDockerInfo(String image, boolean privileged, SingularityDockerNetworkType network, Optional<List<SingularityDockerPortMapping>> portMappings) {
    this(image, privileged, network, portMappings, Optional.<Boolean>absent(), Optional.<Map<String, String>>absent(), Optional.<List<SingularityDockerParameter>>absent());
  }

  @ApiModelProperty(required=true, value="Docker image name")
  public String getImage() {
    return image;
  }

  @ApiModelProperty(required=true, value="Controls use of the docker --privleged flag")
  public boolean isPrivileged()
  {
    return privileged;
  }

  @ApiModelProperty(required=false, value="Docker netowkr type. Value can be BRIDGE, HOST, or NONE", dataType="com.hubspot.mesos.SingularityDockerNetworkType")
  public Optional<SingularityDockerNetworkType> getNetwork() {
    return network;
  }

  @ApiModelProperty(required=false, value="List of port mappings")
  public List<SingularityDockerPortMapping> getPortMappings() {
    return portMappings;
  }

  public boolean hasAllLiteralHostPortMappings() {
    for (SingularityDockerPortMapping mapping : portMappings) {
      if (mapping.getHostPortType() == SingularityPortMappingType.FROM_OFFER) {
        return false;
      }
    }
    return true;
  }

  @JsonIgnore
  public List<Long> getLiteralHostPorts() {
    List<Long> literalHostPorts = new ArrayList<>();
    for (SingularityDockerPortMapping mapping : portMappings) {
      if (mapping.getHostPortType() == SingularityPortMappingType.LITERAL) {
        long port = mapping.getHostPort();
        literalHostPorts.add(port);
      }
    }
    return literalHostPorts;
  }

  @ApiModelProperty(required=false, value="Always run docker pull even if the image already exists locally")
  public boolean isForcePullImage() {
    return forcePullImage;
  }

  @Deprecated
  public Optional<Map<String, String>> getParameters() {
    return parameters;
  }

  @ApiModelProperty(required=false, value="Other docker run command line options to be set")
  public List<SingularityDockerParameter> getDockerParameters() {
    return dockerParameters;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityDockerInfo that = (SingularityDockerInfo) o;
    return privileged == that.privileged &&
        forcePullImage == that.forcePullImage &&
        Objects.equals(image, that.image) &&
        Objects.equals(network, that.network) &&
        Objects.equals(portMappings, that.portMappings) &&
        Objects.equals(parameters, that.parameters) &&
        Objects.equals(dockerParameters, that.dockerParameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(image, privileged, network, portMappings, forcePullImage, parameters, dockerParameters);
  }

  @Override
  public String toString() {
    return "SingularityDockerInfo{" +
        "image='" + image + '\'' +
        ", privileged=" + privileged +
        ", network=" + network +
        ", portMappings=" + portMappings +
        ", forcePullImage=" + forcePullImage +
        ", parameters=" + parameters +
        ", dockerParameters=" + dockerParameters +
        '}';
  }
}
