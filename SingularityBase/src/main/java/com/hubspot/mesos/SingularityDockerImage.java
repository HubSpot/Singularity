package com.hubspot.mesos;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.Beta;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Beta
public class SingularityDockerImage {
  private final String name;

  @JsonCreator
  public SingularityDockerImage(@JsonProperty("name") String name) {
    this.name = name;
  }

  @ApiModelProperty(required=true, value="Docker image name, expected format: [REGISTRY_HOST[:REGISTRY_PORT]/]REPOSITORY[:TAG|@TYPE:DIGEST]")
  public String getName() {
    return name;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityDockerImage that = (SingularityDockerImage) o;
    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return "SingularityDockerImage{" +
        "name='" + name + '\'' +
        '}';
  }
}
