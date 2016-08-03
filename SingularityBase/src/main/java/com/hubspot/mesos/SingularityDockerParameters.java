package com.hubspot.mesos;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonDeserialize(using = SingularityDockerParametersDeserializer.class)
@JsonSerialize(using = SingularityDockerParametersSerializer.class)
public class SingularityDockerParameters {
  private final List<SingularityDockerParameter> parameters;

  @JsonCreator
  public SingularityDockerParameters(@JsonProperty("parameters") List<SingularityDockerParameter> parameters) {
    this.parameters = parameters;
  }

  public List<SingularityDockerParameter> getParameters() {
    return parameters;
  }
}
