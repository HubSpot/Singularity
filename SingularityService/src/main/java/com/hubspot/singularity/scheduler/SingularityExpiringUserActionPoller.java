package com.hubspot.singularity.scheduler;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.expiring.SingularityExpiringBounce;
import com.hubspot.singularity.expiring.SingularityExpiringParent;
import com.hubspot.singularity.expiring.SingularityExpiringPause;
import com.hubspot.singularity.expiring.SingularityExpiringScale;
import com.hubspot.singularity.expiring.SingularityExpiringSkipHealthchecks;
import com.hubspot.singularity.mesos.SingularityMesosModule;

@Singleton
public class SingularityExpiringUserActionPoller extends SingularityLeaderOnlyPoller {

  private final RequestManager requestManager;
  private final List<SingularityExpiringUserActionHandler<?>> handlers;

  @Inject
  SingularityExpiringUserActionPoller(SingularityConfiguration configuration, RequestManager requestManager, @Named(SingularityMesosModule.SCHEDULER_LOCK_NAME) final Lock lock) {
    super(configuration.getCheckExpiringUserActionEveryMillis(), TimeUnit.MILLISECONDS, lock);

    this.requestManager = requestManager;

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
          handleExpiringObject(expiringObject);

          requestManager.deleteExpiringObject(clazz, expiringObject.getRequestId());
        }
      }
    }

    protected abstract void handleExpiringObject(T expiringObject);

  }

  private class SingularityExpiringBounceHandler extends SingularityExpiringUserActionHandler<SingularityExpiringBounce> {

    public SingularityExpiringBounceHandler() {
      super(SingularityExpiringBounce.class);
    }

    @Override
    protected void handleExpiringObject(SingularityExpiringBounce expiringObject) {

    }

  }

  private class SingularityExpiringPauseHandler extends SingularityExpiringUserActionHandler<SingularityExpiringPause> {

    public SingularityExpiringPauseHandler() {
      super(SingularityExpiringPause.class);
    }

    @Override
    protected void handleExpiringObject(SingularityExpiringPause expiringObject) {
      // TODO


    }

  }

  private class SingularityExpiringScaleHandler extends SingularityExpiringUserActionHandler<SingularityExpiringScale> {

    public SingularityExpiringScaleHandler() {
      super(SingularityExpiringScale.class);
    }

    @Override
    protected void handleExpiringObject(SingularityExpiringScale expiringObject) {

    }

  }

  private class SingularityExpiringSkipHealthchecksHandler extends SingularityExpiringUserActionHandler<SingularityExpiringSkipHealthchecks> {

    public SingularityExpiringSkipHealthchecksHandler() {
      super(SingularityExpiringSkipHealthchecks.class);
    }

    @Override
    protected void handleExpiringObject(SingularityExpiringSkipHealthchecks expiringObject) {

    }

  }

}
