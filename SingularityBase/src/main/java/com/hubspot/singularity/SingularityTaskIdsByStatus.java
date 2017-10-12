package com.hubspot.singularity;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityTaskIdsByStatus {
  private List<SingularityTaskId> healthy;
  private List<SingularityTaskId> notYetHealthy;
  private List<SingularityPendingTaskId> pending;
  private List<SingularityTaskId> cleaning;

  @JsonCreator
  public SingularityTaskIdsByStatus(@JsonProperty("healthy") List<SingularityTaskId> healthy,
                                    @JsonProperty("notYetHealthy") List<SingularityTaskId> notYetHealthy,
                                    @JsonProperty("pending") List<SingularityPendingTaskId> pending,
                                    @JsonProperty("cleaning") List<SingularityTaskId> cleaning) {
    this.healthy = healthy;
    this.notYetHealthy = notYetHealthy;
    this.pending = pending;
    this.cleaning = cleaning;
  }

  public List<SingularityTaskId> getHealthy() {
    return healthy;
  }

  public List<SingularityTaskId> getNotYetHealthy() {
    return notYetHealthy;
  }

  public List<SingularityPendingTaskId> getPending() {
    return pending;
  }

  public List<SingularityTaskId> getCleaning() {
    return cleaning;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof SingularityTaskIdsByStatus) {
      final SingularityTaskIdsByStatus that = (SingularityTaskIdsByStatus) obj;
      return Objects.equals(this.healthy, that.healthy) &&
          Objects.equals(this.notYetHealthy, that.notYetHealthy) &&
          Objects.equals(this.pending, that.pending) &&
          Objects.equals(this.cleaning, that.cleaning);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(healthy, notYetHealthy, pending, cleaning);
  }

  @Override
  public String toString() {
    return "SingularityTaskIdsByStatus{" +
        "healthy=" + healthy +
        ", notYetHealthy=" + notYetHealthy +
        ", pending=" + pending +
        ", cleaning=" + cleaning +
        '}';
  }
}
