package com.hubspot.singularity.api.task;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    title = "Holder for a SingularityTaskId",
    subTypes = {
        SingularityTaskMetadata.class,
        SingularityTaskHealthcheckResult.class,
        SingularityTask.class,
        SingularityTaskHistoryUpdate.class
    }
)
public interface SingularityTaskIdHolder {
  @Schema(description = "Task id")
  SingularityTaskId getTaskId();
}
