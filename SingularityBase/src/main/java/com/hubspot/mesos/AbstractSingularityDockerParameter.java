package com.hubspot.mesos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
@JsonDeserialize(as = SingularityDockerParameter.class)
public abstract class AbstractSingularityDockerParameter {
  public abstract String getKey();

  public abstract String getValue();

  @JsonCreator
  public static SingularityDockerParameter fromString(String key) {
    return SingularityDockerParameter.builder().setKey(key).setValue("").build();
  }

  public static List<SingularityDockerParameter> parametersFromMap(Map<String, String> parametersMap) {
    List<SingularityDockerParameter> parameters = new ArrayList<>();
    for (Map.Entry<String, String> entry : parametersMap.entrySet()) {
      parameters.add(SingularityDockerParameter.builder().setKey(entry.getKey()).setValue(entry.getValue()).build());
    }
    return parameters;
  }
}
