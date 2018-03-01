package com.hubspot.singularity.api.task;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;
import com.hubspot.singularity.api.deploy.SingularityDeploy;
import com.hubspot.singularity.api.request.SingularityRequest;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "Describes a task that is waiting to be launched")
public abstract class AbstractSingularityTaskRequest implements Comparable<SingularityTaskRequest> {
  @Schema(description = "The SingularityRequest data used at the time this task was launched")
  public abstract SingularityRequest getRequest();

  @Schema(description = "The full SingularityDeploy data associated with this task")
  public abstract SingularityDeploy getDeploy();

  @Schema(description = "Overrides and settings associated with this particular task")
  public abstract SingularityPendingTask getPendingTask();

  @Override
  public int compareTo(SingularityTaskRequest o) {
    return this.getPendingTask().getPendingTaskId().compareTo(o.getPendingTask().getPendingTaskId());
  }
}
