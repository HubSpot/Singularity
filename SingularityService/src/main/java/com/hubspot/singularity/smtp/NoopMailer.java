package com.hubspot.singularity.smtp;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hubspot.singularity.api.disasters.SingularityDisastersData;
import com.hubspot.singularity.api.expiring.SingularityPauseRequest;
import com.hubspot.singularity.api.request.SingularityRequest;
import com.hubspot.singularity.api.request.SingularityScaleRequest;
import com.hubspot.singularity.api.task.ExtendedTaskState;
import com.hubspot.singularity.api.task.SingularityTask;
import com.hubspot.singularity.api.task.SingularityTaskHistory;
import com.hubspot.singularity.api.task.SingularityTaskId;

public class NoopMailer implements SingularityMailer {
  private static final Logger LOG = LoggerFactory.getLogger(NoopMailer.class);

  private static final NoopMailer INSTANCE = new NoopMailer();

  private NoopMailer() {}

  public static NoopMailer getInstance() {
    return INSTANCE;
  }

  @Override
  public void sendTaskOverdueMail(Optional<SingularityTask> task, SingularityTaskId taskId, SingularityRequest request, long runTime, long expectedRuntime) {
    logNotSendingEmail("task overdue");
  }

  @Override
  public void queueTaskCompletedMail(Optional<SingularityTask> task, SingularityTaskId taskId, SingularityRequest request, ExtendedTaskState taskState) {
    logNotSendingEmail("task completed");
  }

  @Override
  public void sendTaskCompletedMail(SingularityTaskHistory taskHistory, SingularityRequest request) {
    logNotSendingEmail("task completed");
  }

  @Override
  public void sendRequestPausedMail(SingularityRequest request, Optional<SingularityPauseRequest> pauseRequest, Optional<String> user) {
    logNotSendingEmail("request paused");
  }

  @Override
  public void sendRequestUnpausedMail(SingularityRequest request, Optional<String> user, Optional<String> message) {
    logNotSendingEmail("request unpaused");
  }

  @Override
  public void sendRequestScaledMail(SingularityRequest request, Optional<SingularityScaleRequest> newScaleRequest, Optional<Integer> formerInstances, Optional<String> user) {
    logNotSendingEmail("request scaled");
  }

  @Override
  public void sendRequestRemovedMail(SingularityRequest request, Optional<String> user, Optional<String> message) {
    logNotSendingEmail("request removed");
  }

  @Override
  public void sendRequestInCooldownMail(SingularityRequest request) {
    logNotSendingEmail("request in cooldown");
  }

  private void logNotSendingEmail(String type) {
    LOG.debug("Not sending " + type + " mail - no SMTP configuration is present");
  }

  @Override
  public void sendDisasterMail(final SingularityDisastersData disastersData) {
    logNotSendingEmail("new disaster");
  }
}
