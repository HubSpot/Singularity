package com.hubspot.singularity.scheduler;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.api.SingularityScaleRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.expiring.SingularityExpiringBounce;
import com.hubspot.singularity.expiring.SingularityExpiringParent;
import com.hubspot.singularity.expiring.SingularityExpiringPause;
import com.hubspot.singularity.expiring.SingularityExpiringScale;
import com.hubspot.singularity.expiring.SingularityExpiringSkipHealthchecks;
import com.hubspot.singularity.helpers.RequestHelper;
import com.hubspot.singularity.mesos.SingularityMesosModule;
import com.hubspot.singularity.smtp.SingularityMailer;

@Singleton
public class SingularityExpiringUserActionPoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityExpiringUserActionPoller.class);

  private final RequestManager requestManager;
  private final TaskManager taskManager;
  private final SingularityMailer mailer;
  private final RequestHelper requestHelper;
  private final List<SingularityExpiringUserActionHandler<?>> handlers;

  @Inject
  SingularityExpiringUserActionPoller(SingularityConfiguration configuration, RequestManager requestManager, TaskManager taskManager,
      @Named(SingularityMesosModule.SCHEDULER_LOCK_NAME) final Lock lock, RequestHelper requestHelper, SingularityMailer mailer) {
    super(configuration.getCheckExpiringUserActionEveryMillis(), TimeUnit.MILLISECONDS, lock);

    this.requestManager = requestManager;
    this.requestHelper = requestHelper;
    this.mailer = mailer;
    this.taskManager = taskManager;

    List<SingularityExpiringUserActionHandler<?>> tempHandlers = Lists.newArrayList();
    tempHandlers.add(new SingularityExpiringBounceHandler());
    tempHandlers.add(new SingularityExpiringPauseHandler());
    tempHandlers.add(new SingularityExpiringScaleHandler());
    tempHandlers.add(new SingularityExpiringSkipHealthchecksHandler());

    this.handlers = ImmutableList.copyOf(tempHandlers);
  }

  @Override
  public void runActionOnPoll() {
    for (SingularityExpiringUserActionHandler<?> handler : handlers) {
      handler.checkExpiringObjects();
    }
  }

  private abstract class SingularityExpiringUserActionHandler<T extends SingularityExpiringParent<?>> {

    private final Class<T> clazz;

    private SingularityExpiringUserActionHandler(Class<T> clazz) {
      this.clazz = clazz;
    }

    private boolean isExpiringDue(T expiringObject) {
      final long now = System.currentTimeMillis();
      final long duration = now - expiringObject.getStartMillis();

      return duration > expiringObject.getExpiringAPIRequestObject().getDurationMillis().get();
    }

    protected String getMessage(T expiringObject) {
      return String.format("%s expired after %s (%s)", getActionName(),
          JavaUtils.durationFromMillis(expiringObject.getExpiringAPIRequestObject().getDurationMillis().get()),
          expiringObject.getExpiringAPIRequestObject().getMessage().or("No message"));
    }

    protected void checkExpiringObjects() {
      for (T expiringObject : requestManager.getExpiringObjects(clazz)) {
        if (isExpiringDue(expiringObject)) {

          Optional<SingularityRequestWithState> requestWithState = requestManager.getRequest(expiringObject.getRequestId());

          if (!requestWithState.isPresent()) {
            LOG.warn("Request {} not present, discarding {}", expiringObject.getRequestId(), expiringObject);
          } else {
            handleExpiringObject(expiringObject, requestWithState.get(), getMessage(expiringObject));
          }

          requestManager.deleteExpiringObject(clazz, expiringObject.getRequestId());
        }
      }
    }

    protected abstract String getActionName();
    protected abstract void handleExpiringObject(T expiringObject, SingularityRequestWithState requestWithState, String message);

  }

  private class SingularityExpiringBounceHandler extends SingularityExpiringUserActionHandler<SingularityExpiringBounce> {

    public SingularityExpiringBounceHandler() {
      super(SingularityExpiringBounce.class);
    }

    @Override
    protected String getActionName() {
      return "Bounce";
    }

    @Override
    protected void handleExpiringObject(SingularityExpiringBounce expiringObject, SingularityRequestWithState requestWithState, String message) {
      for (SingularityTaskCleanup taskCleanup : taskManager.getCleanupTasks()) {
        if (taskCleanup.getTaskId().getRequestId().equals(expiringObject.getRequestId())
            && taskCleanup.getActionId().isPresent() && expiringObject.getActionId().equals(taskCleanup.getActionId().get())) {
          LOG.info("Discarding cleanup for {} ({}) because of {}", taskCleanup.getTaskId(), taskCleanup, expiringObject);
          taskManager.deleteCleanupTask(taskCleanup.getTaskId().getId());
        }
      }

      Optional<SingularityPendingRequest> pendingRequest = requestManager.getPendingRequest(expiringObject.getRequestId(), expiringObject.getDeployId());

      if (pendingRequest.isPresent() && pendingRequest.get().getActionId().isPresent() && pendingRequest.get().getActionId().get().equals(expiringObject.getActionId())) {
        LOG.info("Discarding pending request for {} ({}) because of {}", expiringObject.getRequestId(), pendingRequest.get(), expiringObject);

        requestManager.deletePendingRequest(pendingRequest.get());
      }

      requestManager.addToPendingQueue(new SingularityPendingRequest(expiringObject.getRequestId(), expiringObject.getDeployId(), System.currentTimeMillis(), expiringObject.getUser(),
          PendingType.CANCEL_BOUNCE, Collections.<String> emptyList(), Optional.<String> absent(), Optional.<Boolean> absent(), Optional.of(message), Optional.of(expiringObject.getActionId())));
    }

  }

  private class SingularityExpiringPauseHandler extends SingularityExpiringUserActionHandler<SingularityExpiringPause> {

    public SingularityExpiringPauseHandler() {
      super(SingularityExpiringPause.class);
    }

    @Override
    protected String getActionName() {
      return "Pause";
    }

    @Override
    protected void handleExpiringObject(SingularityExpiringPause expiringObject, SingularityRequestWithState requestWithState, String message) {
      if (requestWithState.getState() != RequestState.PAUSED) {
        LOG.warn("Discarding {} because request {} is in state {}", expiringObject, requestWithState.getRequest().getId(), requestWithState.getState());
        return;
      }

      LOG.info("Unpausing request {} because of {}", requestWithState.getRequest().getId(), expiringObject);

      requestHelper.unpause(requestWithState.getRequest(), expiringObject.getUser(), Optional.of(message), Optional.<Boolean> absent());
    }

  }

  private class SingularityExpiringScaleHandler extends SingularityExpiringUserActionHandler<SingularityExpiringScale> {

    public SingularityExpiringScaleHandler() {
      super(SingularityExpiringScale.class);
    }

    @Override
    protected String getActionName() {
      return "Scale";
    }

    @Override
    protected void handleExpiringObject(SingularityExpiringScale expiringObject, SingularityRequestWithState requestWithState, String message) {
      final SingularityRequest oldRequest = requestWithState.getRequest();
      final SingularityRequest newRequest = oldRequest.toBuilder().setInstances(expiringObject.getRevertToInstances()).build();

      try {
        requestHelper.updateRequest(newRequest, Optional.of(oldRequest), requestWithState.getState(), expiringObject.getUser(), Optional.<Boolean> absent(), Optional.of(message));

        mailer.sendRequestScaledMail(newRequest, Optional.<SingularityScaleRequest> absent(), oldRequest.getInstances(), expiringObject.getUser());
      } catch (WebApplicationException wae) {
        LOG.error("While trying to apply {} for {}", expiringObject, expiringObject.getRequestId(), wae);
      }
    }

  }

  private class SingularityExpiringSkipHealthchecksHandler extends SingularityExpiringUserActionHandler<SingularityExpiringSkipHealthchecks> {

    public SingularityExpiringSkipHealthchecksHandler() {
      super(SingularityExpiringSkipHealthchecks.class);
    }

    @Override
    protected String getActionName() {
      return "Skip healthchecks";
    }

    @Override
    protected void handleExpiringObject(SingularityExpiringSkipHealthchecks expiringObject, SingularityRequestWithState requestWithState, String message) {
      final SingularityRequest oldRequest = requestWithState.getRequest();
      final SingularityRequest newRequest = oldRequest.toBuilder().setSkipHealthchecks(expiringObject.getRevertToSkipHealthchecks()).build();

      try {
        requestHelper.updateRequest(newRequest, Optional.of(oldRequest), requestWithState.getState(), expiringObject.getUser(), Optional.<Boolean> absent(), Optional.of(message));
      } catch (WebApplicationException wae) {
        LOG.error("While trying to apply {} for {}", expiringObject, expiringObject.getRequestId(), wae);
      }
    }
  }

}
