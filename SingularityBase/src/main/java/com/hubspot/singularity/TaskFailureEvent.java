package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TaskFailureEvent {
  private final int instance;
  private final long timestamp;
  private final TaskFailureType type;

  @JsonCreator
  public TaskFailureEvent(
    @JsonProperty("instance") int instance,
    @JsonProperty("timestamp") long timestamp,
    @JsonProperty("type") TaskFailureType type
  ) {
    this.instance = instance;
    this.timestamp = timestamp;
    this.type = type;
  }

  public int getInstance() {
    return instance;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public TaskFailureType getType() {
    return type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TaskFailureEvent that = (TaskFailureEvent) o;

    if (instance != that.instance) {
      return false;
    }
    if (timestamp != that.timestamp) {
      return false;
    }
    return type == that.type;
  }

  @Override
  public int hashCode() {
    int result = instance;
    result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
    result = 31 * result + (type != null ? type.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return (
      "TaskFailureEvent{" +
      "instance=" +
      instance +
      ", timestamp=" +
      timestamp +
      ", type=" +
      type +
      '}'
    );
  }
}
