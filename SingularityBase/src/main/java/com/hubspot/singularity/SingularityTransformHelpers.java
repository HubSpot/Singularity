package com.hubspot.singularity;

import javax.annotation.Nonnull;

import com.google.common.base.Function;

public class SingularityTransformHelpers {
  private SingularityTransformHelpers() { throw new AssertionError("do not instantiate"); }

  public static final Function<SingularityPendingRequest, String> PENDING_REQUEST_TO_REQUEST_ID = new Function<SingularityPendingRequest, String>() {
    @Override
    public String apply(@Nonnull SingularityPendingRequest input) {
      return input.getRequestId();
    }
  };

  public static final Function<SingularityRequestCleanup, String> REQUEST_CLEANUP_TO_REQUEST_ID = new Function<SingularityRequestCleanup, String>() {
    @Override
    public String apply(@Nonnull SingularityRequestCleanup input) {
      return input.getRequestId();
    }
  };

  public static final Function<SingularityTask, String> TASK_TO_REQUEST_ID = new Function<SingularityTask, String>() {
    @Override
    public String apply(@Nonnull SingularityTask input) {
      return input.getTaskRequest().getRequest().getId();
    }
  };

  public static final Function<SingularityPendingTask, String> PENDING_TASK_TO_REQUEST_ID = new Function<SingularityPendingTask, String>() {
    @Override
    public String apply(@Nonnull SingularityPendingTask input) {
      return input.getPendingTaskId().getRequestId();
    }
  };

  public static final Function<SingularityPendingTaskId, String> PENDING_TASK_ID_TO_REQUEST_ID = new Function<SingularityPendingTaskId, String>() {
    @Override
    public String apply(@Nonnull SingularityPendingTaskId input) {
      return input.getRequestId();
    }
  };

  public static final Function<SingularityTaskCleanup, String> TASK_CLEANUP_TO_REQUEST_ID = new Function<SingularityTaskCleanup, String>() {
    @Override
    public String apply(@Nonnull SingularityTaskCleanup input) {
      return input.getTaskId().getRequestId();
    }
  };

  public static final Function<SingularityTaskId, String> TASK_ID_TO_REQUEST_ID = new Function<SingularityTaskId, String>() {
    @Override
    public String apply(@Nonnull SingularityTaskId input) {
      return input.getRequestId();
    }
  };

  public static final Function<SingularityPendingDeploy, String> PENDING_DEPLOY_TO_REQUEST_ID = new Function<SingularityPendingDeploy, String>() {
    @Override
    public String apply(@Nonnull SingularityPendingDeploy input) {
      return input.getDeployMarker().getRequestId();
    }
  };

  public static final Function<SingularityKilledTaskIdRecord, String> KILLED_TASK_ID_RECORD_TO_REQUEST_ID = new Function<SingularityKilledTaskIdRecord, String>() {
    @Override
    public String apply(@Nonnull SingularityKilledTaskIdRecord input) {
      return input.getTaskId().getRequestId();
    }
  };
}
