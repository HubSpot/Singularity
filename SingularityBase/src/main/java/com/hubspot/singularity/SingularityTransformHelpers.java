package com.hubspot.singularity;

import javax.annotation.Nullable;

import com.google.common.base.Function;

public class SingularityTransformHelpers {
  private SingularityTransformHelpers() { throw new AssertionError("do not instantiate"); }

  public static final Function<SingularityRequest, String> REQUEST_TO_REQUEST_ID = new Function<SingularityRequest, String>() {
    @Nullable
    @Override
    public String apply(SingularityRequest input) {
      return input.getId();
    }
  };

  public static final Function<SingularityPendingRequest, String> PENDING_REQUEST_TO_REQUEST_ID = new Function<SingularityPendingRequest, String>() {
    @Nullable
    @Override
    public String apply(@Nullable SingularityPendingRequest input) {
      return input.getRequestId();
    }
  };

  public static final Function<SingularityRequestCleanup, String> REQUEST_CLEANUP_TO_REQUEST_ID = new Function<SingularityRequestCleanup, String>() {
    @Nullable
    @Override
    public String apply(@Nullable SingularityRequestCleanup input) {
      return input.getRequestId();
    }
  };

  public static final Function<SingularityTask, String> TASK_TO_REQUEST_ID = new Function<SingularityTask, String>() {
    @Nullable
    @Override
    public String apply(@Nullable SingularityTask input) {
      return input.getTaskRequest().getRequest().getId();
    }
  };

  public static final Function<SingularityTaskRequest, String> TASK_REQUEST_TO_REQUEST_ID = new Function<SingularityTaskRequest, String>() {
    @Nullable
    @Override
    public String apply(@Nullable SingularityTaskRequest input) {
      return input.getRequest().getId();
    }
  };

  public static final Function<SingularityPendingTask, String> PENDING_TASK_TO_REQUEST_ID = new Function<SingularityPendingTask, String>() {
    @Nullable
    @Override
    public String apply(@Nullable SingularityPendingTask input) {
      return input.getPendingTaskId().getRequestId();
    }
  };

  public static final Function<SingularityPendingTaskId, String> PENDING_TASK_ID_TO_REQUEST_ID = new Function<SingularityPendingTaskId, String>() {
    @Nullable
    @Override
    public String apply(@Nullable SingularityPendingTaskId input) {
      return input.getRequestId();
    }
  };

  public static final Function<SingularityTaskCleanup, String> TASK_CLEANUP_TO_REQUEST_ID = new Function<SingularityTaskCleanup, String>() {
    @Nullable
    @Override
    public String apply(@Nullable SingularityTaskCleanup input) {
      return input.getTaskId().getRequestId();
    }
  };

  public static final Function<SingularityTaskId, String> TASK_ID_TO_REQUEST_ID = new Function<SingularityTaskId, String>() {
    @Nullable
    @Override
    public String apply(@Nullable SingularityTaskId input) {
      return input.getRequestId();
    }
  };

  public static final Function<SingularityPendingDeploy, String> PENDING_DEPLOY_TO_REQUEST_ID = new Function<SingularityPendingDeploy, String>() {
    @Nullable
    @Override
    public String apply(@Nullable SingularityPendingDeploy input) {
      return input.getDeployMarker().getRequestId();
    }
  };
}
