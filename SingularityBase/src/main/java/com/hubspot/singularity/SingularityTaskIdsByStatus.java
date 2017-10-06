package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;

public class SingularityTaskIdsByStatus {
  private List<SingularityTaskId> healthy;
  private List<SingularityTaskId> notYetHealthy;
  private List<SingularityPendingTaskId> pending;
  private List<SingularityTaskId> cleaning;

  @JsonCreator
  public SingularityTaskIdsByStatus(List<SingularityTaskId> healthy,
                                    List<SingularityTaskId> notYetHealthy,
                                    List<SingularityPendingTaskId> pending,
                                    List<SingularityTaskId> cleaning) {
    this.healthy = healthy;
    this.notYetHealthy = notYetHealthy;
    this.pending = pending;
    this.cleaning = cleaning;
  }
}
