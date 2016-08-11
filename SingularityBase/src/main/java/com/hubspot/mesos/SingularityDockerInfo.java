package com.hubspot.mesos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Optional;

public class SingularityDockerInfo {
  private final String image;
  private final boolean privileged;
  private final Optional<SingularityDockerNetworkType> network;
  private final List<SingularityDockerPortMapping> portMappings;
  private final boolean forcePullImage;
  private final Optional<SingularityDockerParameters> parameters;

  @JsonCreator
  public SingularityDockerInfo(@JsonProperty("image") String image,
                               @JsonProperty("privileged") boolean privileged,
                               @JsonProperty("network") SingularityDockerNetworkType network,
                               @JsonProperty("portMappings") Optional<List<SingularityDockerPortMapping>> portMappings,
                               @JsonProperty("forcePullImage") Optional<Boolean> forcePullImage,
                               @JsonProperty("parameters") Optional<SingularityDockerParameters> parameters) {
    this.image = image;
    this.privileged = privileged;
    this.network = Optional.fromNullable(network);
    this.portMappings = portMappings.or(Collections.<SingularityDockerPortMapping>emptyList());
    this.forcePullImage = forcePullImage.or(false);
    this.parameters = parameters;
  }

  @Deprecated
  public SingularityDockerInfo(String image, boolean privileged, SingularityDockerNetworkType network, Optional<List<SingularityDockerPortMapping>> portMappings) {
    this(image, privileged, network, portMappings, Optional.<Boolean>absent(), Optional.<SingularityDockerParameters>absent());
  }

  public String getImage() {
    return image;
  }

  public boolean isPrivileged()
  {
    return privileged;
  }

  public Optional<SingularityDockerNetworkType> getNetwork() {
    return network;
  }

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

  public boolean isForcePullImage() {
    return forcePullImage;
  }

  public Optional<SingularityDockerParameters> getParameters() {
    return parameters;
  }

  @JsonIgnore
  public List<SingularityDockerParameter> getParametersList() {
    return parameters.isPresent() ? parameters.get().getParameters() : Collections.<SingularityDockerParameter>emptyList();
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
      '}';
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

    if (forcePullImage != that.forcePullImage) {
      return false;
    }
    if (privileged != that.privileged) {
      return false;
    }
    if (image != null ? !image.equals(that.image) : that.image != null) {
      return false;
    }
    if (network != null ? !network.equals(that.network) : that.network != null) {
      return false;
    }
    if (parameters != null ? !parameters.equals(that.parameters) : that.parameters != null) {
      return false;
    }
    if (portMappings != null ? !portMappings.equals(that.portMappings) : that.portMappings != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = image != null ? image.hashCode() : 0;
    result = 31 * result + (privileged ? 1 : 0);
    result = 31 * result + (network != null ? network.hashCode() : 0);
    result = 31 * result + (portMappings != null ? portMappings.hashCode() : 0);
    result = 31 * result + (forcePullImage ? 1 : 0);
    result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
    return result;
  }
}
