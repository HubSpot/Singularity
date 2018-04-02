package com.hubspot.singularity;

import javax.annotation.Nonnull;

import com.google.common.base.Function;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    title = "Holder for a SingularityTaskId",
    subTypes = {
        SingularityTaskMetadata.class,
        SingularityTaskHealthcheckResult.class,
        SingularityTaskHistoryUpdate.class
    }
)
public class SingularityTaskIdHolder {

  private final SingularityTaskId taskId;

  public SingularityTaskIdHolder(SingularityTaskId taskId) {
    this.taskId = taskId;
  }

  @Schema(description = "Task id")
  public SingularityTaskId getTaskId() {
    return taskId;
  }

  public static <T extends SingularityTaskIdHolder> Function<T, SingularityTaskId> getTaskIdFunction() {
    return new Function<T, SingularityTaskId>() {
      @Override
      public SingularityTaskId apply(@Nonnull T value) {
        return value.getTaskId();
      }
    };
  }
}
