package com.hubspot.mesos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

  @JsonIgnore
  public boolean hasDuplicateKey() {
    List<String> keys = new ArrayList<>();
    for (SingularityDockerParameter parameter : parameters) {
      if (keys.contains(parameter.getKey())) {
        return true;
      }
      keys.add(parameter.getKey());
    }
    return false;
  }

  @JsonIgnore
  public Map<String, String> toMap() {
    Map<String, String> map = new HashMap<>();
    for (SingularityDockerParameter parameter : parameters) {
      map.put(parameter.getKey(), parameter.getValue());
    }
    return map;
  }
}
