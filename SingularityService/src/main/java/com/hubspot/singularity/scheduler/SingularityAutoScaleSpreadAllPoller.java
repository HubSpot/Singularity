package com.hubspot.singularity.scheduler;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SlavePlacement;
import com.hubspot.singularity.api.SingularityBounceRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.helpers.RequestHelper;
import com.hubspot.singularity.mesos.SingularityMesosModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Singleton
public class SingularityAutoScaleSpreadAllPoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityAutoScaleSpreadAllPoller.class);

  private final SlaveManager slaveManager;
  private final RequestManager requestManager;
  private final SlavePlacement defaultSlavePlacement;
  private final RequestHelper requestHelper;

  @Inject
  SingularityAutoScaleSpreadAllPoller(SingularityConfiguration configuration, SlaveManager slaveManager, RequestManager requestManager, RequestHelper requestHelper, @Named(SingularityMesosModule.SCHEDULER_LOCK_NAME) final Lock lock) {
    super(configuration.getCheckAutoSpreadAllSlavesEverySeconds(), TimeUnit.SECONDS, lock);
    this.slaveManager = slaveManager;
    this.requestManager = requestManager;
    this.defaultSlavePlacement = configuration.getDefaultSlavePlacement();
    this.requestHelper = requestHelper;
  }

  @Override
  public void runActionOnPoll() {
    int currentActiveSlaveCount = slaveManager.getNumObjectsAtState(MachineState.ACTIVE);

    for (SingularityRequestWithState requestWithState : requestManager.getActiveRequests()) {
      SingularityRequest request = requestWithState.getRequest();
      SlavePlacement placement = request.getSlavePlacement().or(defaultSlavePlacement);
      RequestState state = requestWithState.getState();

      if (placement != SlavePlacement.SPREAD_ALL_SLAVES) {
        continue;
      }

      int requestInstanceCount = request.getInstancesSafe();

      if (requestInstanceCount == currentActiveSlaveCount) {
        LOG.trace("Active Request {} is already spread to all {} available slaves", request.getId(), currentActiveSlaveCount);
      } else {
        LOG.info("Scaling request {} from {} instances to {} available slaves", request.getId(), requestInstanceCount, currentActiveSlaveCount);
        submitScaleRequest(requestWithState, currentActiveSlaveCount);
      }
    }
  }

  private void submitScaleRequest(SingularityRequestWithState oldRequestWithState, Integer newRequestedInstances) {
    SingularityRequest oldRequest = oldRequestWithState.getRequest();
    SingularityRequest newRequest = oldRequest.toBuilder().setInstances(Optional.of((newRequestedInstances))).build();
    Optional<SingularityRequestHistory.RequestHistoryType> historyType = Optional.of(SingularityRequestHistory.RequestHistoryType.SCALED);
    Optional<String> message = Optional.of(String.format("Auto scale number of instances to spread to all %d available slaves", newRequestedInstances));

    requestHelper.updateRequest(newRequest, Optional.of(oldRequest), oldRequestWithState.getState(), historyType, Optional.<String>absent(), oldRequest.getSkipHealthchecks(), message, Optional.<SingularityBounceRequest>absent());
  }

}
