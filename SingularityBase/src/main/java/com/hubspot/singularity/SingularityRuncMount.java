package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityRuncMount {
  private final String type;
  private final String source;
  private final String destination;
  private final String options;

  @JsonCreator
  public SingularityRuncMount(@JsonProperty("type") String type,
                              @JsonProperty("source") String source,
                              @JsonProperty("destination") String destination,
                              @JsonProperty("options") String options) {
    this.type = type;
    this.source = source;
    this.destination = destination;
    this.options = options;
  }

  public String getType() {
    return type;
  }

  public String getSource() {
    return source;
  }

  public String getDestination() {
    return destination;
  }

  public String getOptions() {
    return options;
  }
}
