package com.hubspot.singularity.scheduler;

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
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.api.SingularityScaleRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.RequestManager;
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
  private final SingularityMailer mailer;
  private final RequestHelper requestHelper;
  private final List<SingularityExpiringUserActionHandler<?>> handlers;

  // TODO not sure if this needs a lock.
  @Inject
  SingularityExpiringUserActionPoller(SingularityConfiguration configuration, RequestManager requestManager, @Named(SingularityMesosModule.SCHEDULER_LOCK_NAME) final Lock lock,
      RequestHelper requestHelper, SingularityMailer mailer) {
    super(configuration.getCheckExpiringUserActionEveryMillis(), TimeUnit.MILLISECONDS, lock);

    this.requestManager = requestManager;
    this.requestHelper = requestHelper;
    this.mailer = mailer;

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

  private abstract class SingularityExpiringUserActionHandler<T extends SingularityExpiringParent> {

    private final Class<T> clazz;

    private SingularityExpiringUserActionHandler(Class<T> clazz) {
      this.clazz = clazz;
    }

    private boolean isExpiringDue(T expiringObject) {
      final long now = System.currentTimeMillis();
      final long duration = now - expiringObject.getStartMillis();

      return duration > expiringObject.getDurationMillis();
    }

    protected void checkExpiringObjects() {
      for (T expiringObject : requestManager.getExpiringObjects(clazz)) {
        if (isExpiringDue(expiringObject)) {

          Optional<SingularityRequestWithState> requestWithState = requestManager.getRequest(expiringObject.getRequestId());

          if (!requestWithState.isPresent()) {
            LOG.warn("Request {} not present, discarding {}", expiringObject.getRequestId(), expiringObject);
          } else {
            handleExpiringObject(expiringObject, requestWithState.get());
          }

          requestManager.deleteExpiringObject(clazz, expiringObject.getRequestId());
        }
      }
    }

    protected abstract void handleExpiringObject(T expiringObject, SingularityRequestWithState requestWithState);

  }

  private class SingularityExpiringBounceHandler extends SingularityExpiringUserActionHandler<SingularityExpiringBounce> {

    public SingularityExpiringBounceHandler() {
      super(SingularityExpiringBounce.class);
    }

    @Override
    protected void handleExpiringObject(SingularityExpiringBounce expiringObject, SingularityRequestWithState requestWithState) {

    }

  }

  private class SingularityExpiringPauseHandler extends SingularityExpiringUserActionHandler<SingularityExpiringPause> {

    public SingularityExpiringPauseHandler() {
      super(SingularityExpiringPause.class);
    }

    @Override
    protected void handleExpiringObject(SingularityExpiringPause expiringObject, SingularityRequestWithState requestWithState) {
      if (requestWithState.getState() != RequestState.PAUSED) {
        LOG.warn("Discarding {} because request {} is in state {}", expiringObject, requestWithState.getRequest().getId(), requestWithState.getState());
        return;
      }

      LOG.info("Unpausing request {} because of {}", requestWithState.getRequest().getId(), expiringObject);

      requestHelper.unpause(requestWithState.getRequest(), expiringObject.getUser());
    }

  }

  private class SingularityExpiringScaleHandler extends SingularityExpiringUserActionHandler<SingularityExpiringScale> {

    public SingularityExpiringScaleHandler() {
      super(SingularityExpiringScale.class);
    }

    @Override
    protected void handleExpiringObject(SingularityExpiringScale expiringObject, SingularityRequestWithState requestWithState) {
      final SingularityRequest oldRequest = requestWithState.getRequest();
      final SingularityRequest newRequest = oldRequest.toBuilder().setInstances(expiringObject.getRevertToInstances()).build();

      try {
        requestHelper.updateRequest(newRequest, Optional.of(oldRequest), requestWithState.getState(), expiringObject.getUser(), Optional.<Boolean> absent());

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
    protected void handleExpiringObject(SingularityExpiringSkipHealthchecks expiringObject, SingularityRequestWithState requestWithState) {
      final SingularityRequest oldRequest = requestWithState.getRequest();
      final SingularityRequest newRequest = oldRequest.toBuilder().setSkipHealthchecks(expiringObject.getRevertToSkipHealthchecks()).build();

      try {
        requestHelper.updateRequest(newRequest, Optional.of(oldRequest), requestWithState.getState(), expiringObject.getUser(), Optional.<Boolean> absent());
      } catch (WebApplicationException wae) {
        LOG.error("While trying to apply {} for {}", expiringObject, expiringObject.getRequestId(), wae);
      }
    }

  }

}
