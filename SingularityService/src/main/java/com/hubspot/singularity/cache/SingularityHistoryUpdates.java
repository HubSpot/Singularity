package com.hubspot.singularity.cache;


import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;

public class SingularityHistoryUpdates {
  private final Map<ExtendedTaskState, SingularityTaskHistoryUpdate> historyUpdates;

  @JsonCreator
  public SingularityHistoryUpdates(@JsonProperty("historyUpdates") Map<ExtendedTaskState, SingularityTaskHistoryUpdate> historyUpdates) {
    this.historyUpdates = historyUpdates;
  }

  public Map<ExtendedTaskState, SingularityTaskHistoryUpdate> getHistoryUpdates() {
    return historyUpdates;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SingularityHistoryUpdates that = (SingularityHistoryUpdates) o;

    return historyUpdates != null ? historyUpdates.equals(that.historyUpdates) : that.historyUpdates == null;
  }

  @Override
  public int hashCode() {
    return historyUpdates != null ? historyUpdates.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "SingularityHistoryUpdates{" +
        "historyUpdates=" + historyUpdates +
        '}';
  }
}
