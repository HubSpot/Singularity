package com.hubspot.singularity.api.task;

import java.util.Comparator;

import org.immutables.value.Value.Immutable;

import com.google.common.primitives.Doubles;
import com.hubspot.singularity.annotations.SingularityStyle;

@Immutable
@SingularityStyle
public interface SingularityTaskRequestWithPriorityIF {

  static Comparator<SingularityTaskRequestWithPriority> weightedPriorityComparator() {
    return (o1, o2) -> Doubles.compare(o2.getWeightedPriority(), o1.getWeightedPriority());
  }

  SingularityTaskRequest getTaskRequest();

  double getWeightedPriority();
}
