package com.hubspot.singularity.athena;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AthenaField {
  private final String name;
  private final String type; // TODO enum this

  @JsonCreator
  public AthenaField(@JsonProperty("name") String name, @JsonProperty("type") String type) {
    this.name = name;
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  @JsonIgnore
  public String toTableCreateString() {
    return String.format("%s %s", name, type);
  }
}
