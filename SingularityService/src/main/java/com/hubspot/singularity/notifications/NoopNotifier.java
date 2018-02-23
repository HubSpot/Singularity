package com.hubspot.singularity.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityDisastersData;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.api.SingularityPauseRequest;
import com.hubspot.singularity.api.SingularityScaleRequest;

public class NoopNotifier implements SingularityNotifier {
  private static final Logger LOG = LoggerFactory.getLogger(NoopNotifier.class);
  private final Class<? extends SingularityNotifier> disabledNotifierClass;

  public NoopNotifier(Class<? extends SingularityNotifier> disabledNotifierClass) {
    this.disabledNotifierClass = disabledNotifierClass;
  }

  @Override
  public void sendTaskOverdueNotification(Optional<SingularityTask> task, SingularityTaskId taskId, SingularityRequest request, long runTime, long expectedRuntime) {
    logNotSendingNotification("task overdue");
  }

  @Override
  public void sendTaskFinishedNotification(SingularityTaskHistory taskHistory, SingularityRequest request) {
    logNotSendingNotification("task completed");
  }

  @Override
  public void sendRequestPausedNotification(SingularityRequest request, Optional<SingularityPauseRequest> pauseRequest, Optional<String> user) {
    logNotSendingNotification("request paused");
  }

  @Override
  public void sendRequestUnpausedNotification(SingularityRequest request, Optional<String> user, Optional<String> message) {
    logNotSendingNotification("request unpaused");
  }

  @Override
  public void sendRequestScaledNotification(SingularityRequest request, Optional<SingularityScaleRequest> newScaleRequest, Optional<Integer> formerInstances, Optional<String> user) {
    logNotSendingNotification("request scaled");
  }

  @Override
  public void sendRequestRemovedNotification(SingularityRequest request, Optional<String> user, Optional<String> message) {
    logNotSendingNotification("request removed");
  }

  @Override
  public void sendRequestInCooldownNotification(SingularityRequest request) {
    logNotSendingNotification("request in cooldown");
  }

  private void logNotSendingNotification(String type) {
    LOG.debug("Not sending {} via {} notifier because it is not configured", type, disabledNotifierClass.getSimpleName());
  }

  @Override
  public void sendDisasterNotification(final SingularityDisastersData disastersData) {
    logNotSendingNotification("new disaster");
  }

  @Override
  public boolean shouldNotify(SingularityRequest request) {
    return true;
  }

}
