package com.hubspot.mesos;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SingularityContainerType {
  MESOS, DOCKER;

  @JsonCreator
  public static SingularityContainerType fromString(String name) {
    return valueOf(name.toUpperCase());
  }
}
