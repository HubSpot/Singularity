package com.hubspot.mesos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import java.util.Collections;
import java.util.List;

public class SingularityDockerInfo {
  private final String image;
  private final boolean privileged;
  private final Optional<SingularityDockerNetworkType> network;
  private final List<SingularityDockerPortMapping> portMappings;

  @JsonCreator
  public SingularityDockerInfo(@JsonProperty("image") String image,
                               @JsonProperty("privileged") boolean privileged,
                               @JsonProperty("network") Optional<SingularityDockerNetworkType> network,
                               @JsonProperty("portMappings") Optional<List<SingularityDockerPortMapping>> portMappings) {
    this.image = image;
    this.privileged = privileged;
    this.network = network;
    this.portMappings = portMappings.or(Collections.<SingularityDockerPortMapping>emptyList());
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

  @Override
  public String toString() {
    return String.format("DockerInfo [image=%s, network=%s, portMappings=%s]", image, network, portMappings);
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

    if (!image.equals(that.image)) {
      return false;
    }
    if (privileged != that.privileged) {
      return false;
    }
    if (!network.equals(that.network)) {
      return false;
    }
    if (!portMappings.equals(that.portMappings)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = image.hashCode();
    result = 31 * result + (privileged ? 1 : 0);
    result = 31 * result + network.hashCode();
    result = 31 * result + portMappings.hashCode();
    return result;
  }
}
