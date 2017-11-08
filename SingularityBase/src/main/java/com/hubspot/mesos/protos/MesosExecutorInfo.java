package com.hubspot.mesos.protos;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class MesosExecutorInfo {
  private final Optional<MesosStringValue> executorId;
  private final Map<String, Object> allOtherFields;

  @JsonCreator
  public MesosExecutorInfo(@JsonProperty("executorId") Optional<MesosStringValue> executorId) {
    this.executorId = executorId;
    this.allOtherFields = new HashMap<>();
  }

  public MesosStringValue getExecutorId() {
    return executorId.orNull();
  }

  public boolean hasExecutorId() {
    return executorId.isPresent();
  }

  // Unknown fields
  @JsonAnyGetter
  public Map<String, Object> getUnknownFields() {
    return allOtherFields;
  }

  @JsonAnySetter
  public void setUnknownFields(String name, Object value) {
    allOtherFields.put(name, value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof MesosExecutorInfo) {
      final MesosExecutorInfo that = (MesosExecutorInfo) obj;
      return Objects.equals(this.executorId, that.executorId) &&
          Objects.equals(this.allOtherFields, that.allOtherFields);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(executorId, allOtherFields);
  }

  @Override
  public String toString() {
    return "MesosExecutorInfo{" +
        "executorId=" + executorId +
        ", allOtherFields=" + allOtherFields +
        '}';
  }
}
