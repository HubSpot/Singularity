package com.hubspot.singularity.smtp;

import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.SingularityDisastersData;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.api.SingularityPauseRequest;
import com.hubspot.singularity.api.SingularityScaleRequest;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoopMailer implements SingularityMailer {

  private static final Logger LOG = LoggerFactory.getLogger(NoopMailer.class);

  private static final NoopMailer INSTANCE = new NoopMailer();

  private NoopMailer() {}

  public static NoopMailer getInstance() {
    return INSTANCE;
  }

  @Override
  public void sendTaskOverdueMail(
    Optional<SingularityTask> task,
    SingularityTaskId taskId,
    SingularityRequest request,
    long runTime,
    long expectedRuntime
  ) {
    logNotSendingEmail("task overdue");
  }

  @Override
  public void queueTaskCompletedMail(
    Optional<SingularityTask> task,
    SingularityTaskId taskId,
    SingularityRequest request,
    ExtendedTaskState taskState
  ) {
    logNotSendingEmail("task completed");
  }

  @Override
  public void sendTaskCompletedMail(
    SingularityTaskHistory taskHistory,
    SingularityRequest request
  ) {
    logNotSendingEmail("task completed");
  }

  @Override
  public void sendRequestPausedMail(
    SingularityRequest request,
    Optional<SingularityPauseRequest> pauseRequest,
    String user
  ) {
    logNotSendingEmail("request paused");
  }

  @Override
  public void sendRequestUnpausedMail(
    SingularityRequest request,
    String user,
    Optional<String> message
  ) {
    logNotSendingEmail("request unpaused");
  }

  @Override
  public void sendRequestScaledMail(
    SingularityRequest request,
    Optional<SingularityScaleRequest> newScaleRequest,
    Optional<Integer> formerInstances,
    String user
  ) {
    logNotSendingEmail("request scaled");
  }

  @Override
  public void sendRequestRemovedMail(
    SingularityRequest request,
    String user,
    Optional<String> message
  ) {
    logNotSendingEmail("request removed");
  }

  @Override
  public void sendRequestInCooldownMail(SingularityRequest request) {
    logNotSendingEmail("request in cooldown");
  }

  private void logNotSendingEmail(String type) {
    LOG.debug("Not sending {}} mail - no SMTP configuration is present", type);
  }

  @Override
  public void sendDisasterMail(final SingularityDisastersData disastersData) {
    logNotSendingEmail("new disaster");
  }
}
