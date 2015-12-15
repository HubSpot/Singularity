package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.inject.Singleton;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.mesos.SingularityMesosModule;

@Singleton
public class SingularityRequestTimedUserActionPoller extends SingularityLeaderOnlyPoller {

  private final RequestManager requestManager;

  @Inject
  SingularityRequestTimedUserActionPoller(SingularityConfiguration configuration, RequestManager requestManager, @Named(SingularityMesosModule.SCHEDULER_LOCK_NAME) final Lock lock) {
    super(configuration.getCheckTimedUserActionEveryMillis(), TimeUnit.MILLISECONDS, lock);

    this.requestManager = requestManager;
  }

  @Override
  public void runActionOnPoll() {


  }

}
