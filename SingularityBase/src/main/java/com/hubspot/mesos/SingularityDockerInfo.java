package com.hubspot.mesos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityDockerInfo {
  private final String image;

  @JsonCreator
  public SingularityDockerInfo(@JsonProperty("image") String image) {
    this.image = image;
  }

  public String getImage() {
    return image;
  }

  @Override
  public String toString() {
    return String.format("DockerInfo [image=%s]", image);
  }
}
