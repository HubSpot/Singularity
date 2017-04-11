package com.hubspot.singularity.mesos;

import java.util.List;

public class SingularityOfferProcessingResult {
  private final int tasksScheduled;
  private final int tasksRemaining;
  private final List<SingularityOfferHolder> offerHolders;

  public SingularityOfferProcessingResult(int tasksScheduled, int tasksRemaining, List<SingularityOfferHolder> offerHolders) {
    this.tasksScheduled = tasksScheduled;
    this.tasksRemaining = tasksRemaining;
    this.offerHolders = offerHolders;
  }

  public int getTasksScheduled() {
    return tasksScheduled;
  }

  public int getTasksRemaining() {
    return tasksRemaining;
  }

  public List<SingularityOfferHolder> getOfferHolders() {
    return offerHolders;
  }
}
