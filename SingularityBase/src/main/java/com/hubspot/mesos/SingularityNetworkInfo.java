package com.hubspot.mesos;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Beta
public class SingularityNetworkInfo {
  private final Optional<String> name;
  private final Optional<List<String>> groups;
  private final Optional<List<SingularityPortMapping>> portMappings;

  @JsonCreator
  public SingularityNetworkInfo(@JsonProperty("name") Optional<String> name,
      @JsonProperty("groups") Optional<List<String>> groups,
      @JsonProperty("portMappings") Optional<List<SingularityPortMapping>> portMappings) {
    this.name = name;
    this.groups = groups;
    this.portMappings = portMappings;
  }

  @ApiModelProperty(required=false, value="Name of the network for the network driver to use")
  public Optional<String> getName() {
    return name;
  }

  @ApiModelProperty(required=false, value="List of network groups for the container")
  public Optional<List<String>> getGroups() {
    return groups;
  }

  @ApiModelProperty(required=false, value="List of ip port mappings to expose")
  public Optional<List<SingularityPortMapping>> getPortMappings() {
    return portMappings;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityNetworkInfo that = (SingularityNetworkInfo) o;
    return Objects.equals(name, that.name) &&
        Objects.equals(groups, that.groups) &&
        Objects.equals(portMappings, that.portMappings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, groups, portMappings);
  }

  @Override
  public String toString() {
    return "SingularityNetworkInfo{" +
        "name='" + name + '\'' +
        ", groups=" + groups +
        ", portMappings=" + portMappings +
        '}';
  }
}
