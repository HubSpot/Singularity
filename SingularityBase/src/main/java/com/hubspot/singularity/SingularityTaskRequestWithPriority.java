package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.primitives.Doubles;
import java.util.Comparator;

public class SingularityTaskRequestWithPriority {

  private final SingularityTaskRequest taskRequest;
  private final double weightedPriority;

  @JsonCreator
  public SingularityTaskRequestWithPriority(
    @JsonProperty("taskRequest") SingularityTaskRequest taskRequest,
    double weightedPriority
  ) {
    this.taskRequest = taskRequest;
    this.weightedPriority = weightedPriority;
  }

  public static Comparator<SingularityTaskRequestWithPriority> weightedPriorityComparator() {
    return (o1, o2) ->
      Doubles.compare(o2.getWeightedPriority(), o1.getWeightedPriority());
  }

  public SingularityTaskRequest getTaskRequest() {
    return taskRequest;
  }

  public double getWeightedPriority() {
    return weightedPriority;
  }
}
