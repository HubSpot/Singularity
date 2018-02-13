package com.hubspot.singularity.notifications;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityDisastersData;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.api.SingularityPauseRequest;
import com.hubspot.singularity.api.SingularityScaleRequest;

public interface SingularityNotifier {
  void sendTaskOverdueNotification(final Optional<SingularityTask> task, final SingularityTaskId taskId, final SingularityRequest request, final long runTime, final long expectedRuntime);
  void sendTaskFinishedNotification(SingularityTaskHistory taskHistory, SingularityRequest request);
  void sendRequestPausedNotification(SingularityRequest request, Optional<SingularityPauseRequest> pauseRequest, Optional<String> user);
  void sendRequestUnpausedNotification(SingularityRequest request, Optional<String> user, Optional<String> message);
  void sendRequestScaledNotification(SingularityRequest request, Optional<SingularityScaleRequest> newScaleRequest, Optional<Integer> formerInstances, Optional<String> user);
  void sendRequestRemovedNotification(SingularityRequest request, Optional<String> user, Optional<String> message);
  void sendRequestInCooldownNotification(final SingularityRequest request);
  void sendDisasterNotification(final SingularityDisastersData disastersData);
  boolean shouldNotify(SingularityRequest request);
}
