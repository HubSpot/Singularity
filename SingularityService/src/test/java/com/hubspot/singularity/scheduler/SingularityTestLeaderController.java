package com.hubspot.singularity.scheduler;

import com.google.common.net.HostAndPort;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityLeaderController;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.StateManager;
import com.hubspot.singularity.mesos.SingularityMesosOfferManager;
import com.hubspot.singularity.mesos.SingularityMesosScheduler;
import com.hubspot.singularity.mesos.SingularityPendingTaskQueueProcessor;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

public class SingularityTestLeaderController extends SingularityLeaderController {

  @Inject
  public SingularityTestLeaderController(
    StateManager stateManager,
    SingularityConfiguration configuration,
    SingularityAbort abort,
    SingularityExceptionNotifier exceptionNotifier,
    @Named(SingularityMainModule.HTTP_HOST_AND_PORT) HostAndPort hostAndPort,
    SingularityMesosScheduler scheduler,
    SingularityMesosOfferManager singularityMesosOfferManager,
    SingularityPendingTaskQueueProcessor pendingTaskQueueProcessor
  ) {
    super(
      stateManager,
      configuration,
      abort,
      exceptionNotifier,
      hostAndPort,
      scheduler,
      singularityMesosOfferManager,
      pendingTaskQueueProcessor
    );
  }

  @Override
  public boolean isTestMode() {
    return true;
  }
}
