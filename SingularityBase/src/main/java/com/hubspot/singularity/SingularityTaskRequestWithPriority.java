package com.hubspot.singularity;

import java.util.Comparator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.primitives.Doubles;

public class SingularityTaskRequestWithPriority {
  private final SingularityTaskRequest taskRequest;
  private final double weightedPriority;

  @JsonCreator
  public SingularityTaskRequestWithPriority(@JsonProperty("taskRequest") SingularityTaskRequest taskRequest, double weightedPriority) {
    this.taskRequest = taskRequest;
    this.weightedPriority = weightedPriority;
  }

  public static Comparator<SingularityTaskRequestWithPriority> weightedPriorityComparator() {
    return new Comparator<SingularityTaskRequestWithPriority>() {
      @Override public int compare(SingularityTaskRequestWithPriority o1, SingularityTaskRequestWithPriority o2) {
        return Doubles.compare(o2.getWeightedPriority(), o1.getWeightedPriority());
      }
    };
  }

  public SingularityTaskRequest getTaskRequest() {
    return taskRequest;
  }

  public double getWeightedPriority() {
    return weightedPriority;
  }
}
