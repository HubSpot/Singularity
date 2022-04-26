package com.hubspot.mesos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Schema(description = "A label for a mesos task")
public class SingularityMesosTaskLabel {

  private final String key;
  private final Optional<String> value;

  @JsonCreator
  public static SingularityMesosTaskLabel fromString(String value) {
    return new SingularityMesosTaskLabel(value, Optional.<String>empty());
  }

  @JsonCreator
  public SingularityMesosTaskLabel(
    @JsonProperty("key") String key,
    @JsonProperty("value") Optional<String> value
  ) {
    this.key = key;
    this.value = value;
  }

  @Schema(description = "Label key")
  public String getKey() {
    return key;
  }

  @Schema(description = "Optional label value", nullable = true)
  public Optional<String> getValue() {
    return value;
  }

  public static List<SingularityMesosTaskLabel> labelsFromMap(
    Map<String, String> parametersMap
  ) {
    List<SingularityMesosTaskLabel> labels = new ArrayList<>();
    for (Map.Entry<String, String> entry : parametersMap.entrySet()) {
      labels.add(
        new SingularityMesosTaskLabel(entry.getKey(), Optional.of(entry.getValue()))
      );
    }
    return labels;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityMesosTaskLabel that = (SingularityMesosTaskLabel) o;
    return Objects.equals(key, that.key) && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value);
  }

  @Override
  public String toString() {
    return "SingularityMesosTaskLabel{" + "key='" + key + '\'' + ", value=" + value + '}';
  }
}
