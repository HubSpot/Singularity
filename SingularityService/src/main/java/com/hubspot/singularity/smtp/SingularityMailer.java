package com.hubspot.singularity.smtp;

import com.google.common.base.Optional;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.SingularityDisastersData;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.api.SingularityPauseRequest;
import com.hubspot.singularity.api.SingularityScaleRequest;

public interface SingularityMailer {
  void sendTaskOverdueMail(final Optional<SingularityTask> task, final SingularityTaskId taskId, final SingularityRequest request, final long runTime, final long expectedRuntime);
  void queueTaskCompletedMail(final Optional<SingularityTask> task, final SingularityTaskId taskId, final SingularityRequest request, final ExtendedTaskState taskState);
  void sendTaskCompletedMail(SingularityTaskHistory taskHistory, SingularityRequest request);
  void sendRequestPausedMail(SingularityRequest request, Optional<SingularityPauseRequest> pauseRequest, Optional<String> user);
  void sendRequestUnpausedMail(SingularityRequest request, Optional<String> user, Optional<String> message);
  void sendRequestScaledMail(SingularityRequest request, Optional<SingularityScaleRequest> newScaleRequest, Optional<Integer> formerInstances, Optional<String> user);
  void sendRequestRemovedMail(SingularityRequest request, Optional<String> user, Optional<String> message);
  void sendRequestInCooldownMail(final SingularityRequest request);
  void sendDisasterMail(final SingularityDisastersData disastersData);
}
