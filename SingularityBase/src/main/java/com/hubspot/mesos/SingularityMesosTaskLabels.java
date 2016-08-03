package com.hubspot.mesos;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

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
}
