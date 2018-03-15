package com.hubspot.singularity.api.task;

import java.util.Optional;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;
import com.hubspot.singularity.api.common.SingularityFrameworkMessage;

@Immutable
@SingularityStyle
public abstract class AbstractSingularityTaskDestroyFrameworkMessage extends SingularityFrameworkMessage {

  public abstract SingularityTaskId getTaskId();

  public abstract Optional<String> getUser();
}
