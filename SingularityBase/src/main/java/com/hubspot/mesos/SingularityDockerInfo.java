package com.hubspot.mesos;

import org.apache.mesos.Protos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SingularityDockerInfo {
  private final String image;
  private final boolean privileged;
  private final Optional<SingularityDockerNetworkType> network;
  private final List<SingularityDockerPortMapping> portMappings;
  private final boolean forcePullImage;
  private final Map<String, String> parameters;

  @JsonCreator
  public SingularityDockerInfo(@JsonProperty("image") String image,
                               @JsonProperty("privileged") boolean privileged,
                               @JsonProperty("network") SingularityDockerNetworkType network,
                               @JsonProperty("portMappings") Optional<List<SingularityDockerPortMapping>> portMappings,
                               @JsonProperty("forcePullImage") boolean forcePullImage,
                               @JsonProperty("parameters") Optional<Map<String, String>> parameters) {
    this.image = image;
    this.privileged = privileged;
    this.network = Optional.fromNullable(network);
    this.portMappings = portMappings.or(Collections.<SingularityDockerPortMapping>emptyList());
    this.forcePullImage = forcePullImage;
    this.parameters = parameters.or(Collections.<String, String>emptyMap());
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

  public boolean isForcePullImage() {
    return forcePullImage;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }

  @Override
  public String toString() {
    return String.format("DockerInfo [image=%s, network=%s, portMappings=%s, privileged=%s, forcePullImage=%s, parameters=%s]", image, network, portMappings, privileged, forcePullImage, parameters);
  }
}