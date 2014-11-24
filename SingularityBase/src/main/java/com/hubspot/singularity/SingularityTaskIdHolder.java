package com.hubspot.singularity;

import javax.annotation.Nonnull;

import com.google.common.base.Function;

public class SingularityTaskIdHolder {

  private final SingularityTaskId taskId;

  public SingularityTaskIdHolder(SingularityTaskId taskId) {
    this.taskId = taskId;
  }

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
