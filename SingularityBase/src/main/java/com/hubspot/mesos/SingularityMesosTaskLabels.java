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
import com.google.common.base.Optional;

@JsonDeserialize(using = SingularityMesosTaskLabelsDeserializer.class)
@JsonSerialize(using = SingularityMesosTaskLabelsSerializer.class)
public class SingularityMesosTaskLabels {
  private final List<SingularityMesosTaskLabel> labels;

  @JsonCreator
  public SingularityMesosTaskLabels(@JsonProperty("labels") List<SingularityMesosTaskLabel> labels) {
    this.labels = labels;
  }

  public List<SingularityMesosTaskLabel> getLabels() {
    return labels;
  }

  @JsonIgnore
  public boolean hasDuplicateKey() {
    List<String> keys = new ArrayList<>();
    for (SingularityMesosTaskLabel label : labels) {
      if (keys.contains(label.getKey())) {
        return true;
      }
      keys.add(label.getKey());
    }
    return false;
  }

  @JsonIgnore
  public Map<String, Optional<String>> toMap() {
    Map<String, Optional<String>> map = new HashMap<>();
    for (SingularityMesosTaskLabel label : labels) {
      map.put(label.getKey(), label.getValue());
    }
    return map;
  }
}
