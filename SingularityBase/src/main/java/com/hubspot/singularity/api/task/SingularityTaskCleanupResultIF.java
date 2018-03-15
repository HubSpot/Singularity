package com.hubspot.singularity.api.task;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;
import com.hubspot.singularity.api.common.SingularityCreateResult;

@Immutable
@SingularityStyle
public interface SingularityTaskCleanupResultIF {
  SingularityCreateResult getResult();

  SingularityTask getTask();
}
