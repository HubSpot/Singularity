package com.hubspot.mesos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
public abstract class AbstractSingularityMesosTaskLabel {
  public abstract String getKey();

  public abstract Optional<String> getValue();

  @JsonCreator
  public static SingularityMesosTaskLabel fromString(String key) {
    return SingularityMesosTaskLabel.builder().setKey(key).build();
  }

  public static List<SingularityMesosTaskLabel> labelsFromMap(Map<String, String> parametersMap) {
    List<SingularityMesosTaskLabel> labels = new ArrayList<>();
    for (Map.Entry<String, String> entry : parametersMap.entrySet()) {
      labels.add(SingularityMesosTaskLabel.builder().setKey(entry.getKey()).setValue(entry.getValue()).build());
    }
    return labels;
  }
}
