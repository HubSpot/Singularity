package com.hubspot.singularity.scheduler;

import com.google.inject.Inject;
import com.hubspot.singularity.AgentPlacement;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.api.SingularityBounceRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.AgentManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.helpers.RequestHelper;
import com.hubspot.singularity.mesos.SingularitySchedulerLock;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SingularityAutoScaleSpreadAllPoller extends SingularityLeaderOnlyPoller {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityAutoScaleSpreadAllPoller.class
  );

  private final AgentManager agentManager;
  private final RequestManager requestManager;
  private final AgentPlacement defaultAgentPlacement;
  private final RequestHelper requestHelper;
  private final boolean spreadAllSlavesEnabled;
  private final SingularitySchedulerLock lock;

  @Inject
  SingularityAutoScaleSpreadAllPoller(
    SingularityConfiguration configuration,
    AgentManager agentManager,
    RequestManager requestManager,
    RequestHelper requestHelper,
    SingularitySchedulerLock lock
  ) {
    super(configuration.getCheckAutoSpreadAllAgentsEverySeconds(), TimeUnit.SECONDS);
    this.agentManager = agentManager;
    this.requestManager = requestManager;
    this.defaultAgentPlacement = configuration.getDefaultAgentPlacement();
    this.requestHelper = requestHelper;
    this.spreadAllSlavesEnabled = configuration.isSpreadAllAgentsEnabled();
    this.lock = lock;
  }

  @Override
  public void runActionOnPoll() {
    int currentActiveSlaveCount = agentManager.getNumObjectsAtState(MachineState.ACTIVE);

    for (SingularityRequestWithState requestWithState : requestManager.getActiveRequests()) {
      lock.runWithRequestLock(
        () -> {
          SingularityRequest request = requestWithState.getRequest();
          // TODO - support global override here?
          AgentPlacement placement = request
            .getAgentPlacement()
            .orElse(defaultAgentPlacement);

          if (
            placement != AgentPlacement.SPREAD_ALL_SLAVES &&
            placement != AgentPlacement.SPREAD_ALL_AGENTS
          ) {
            return;
          }

          int requestInstanceCount = request.getInstancesSafe();

          if (requestInstanceCount == currentActiveSlaveCount) {
            LOG.trace(
              "Active Request {} is already spread to all {} available slaves",
              request.getId(),
              currentActiveSlaveCount
            );
          } else {
            LOG.info(
              "Scaling request {} from {} instances to {} available slaves",
              request.getId(),
              requestInstanceCount,
              currentActiveSlaveCount
            );
            submitScaleRequest(requestWithState, currentActiveSlaveCount);
          }
        },
        requestWithState.getRequest().getId(),
        getClass().getSimpleName()
      );
    }
  }

  private void submitScaleRequest(
    SingularityRequestWithState oldRequestWithState,
    Integer newRequestedInstances
  ) {
    SingularityRequest oldRequest = oldRequestWithState.getRequest();
    SingularityRequest newRequest = oldRequest
      .toBuilder()
      .setInstances(Optional.of((newRequestedInstances)))
      .build();
    Optional<SingularityRequestHistory.RequestHistoryType> historyType = Optional.of(
      SingularityRequestHistory.RequestHistoryType.SCALED
    );
    Optional<String> message = Optional.of(
      String.format(
        "Auto scale number of instances to spread to all %d available slaves",
        newRequestedInstances
      )
    );

    requestHelper.updateRequest(
      newRequest,
      Optional.of(oldRequest),
      oldRequestWithState.getState(),
      historyType,
      Optional.<String>empty(),
      oldRequest.getSkipHealthchecks(),
      message,
      Optional.<SingularityBounceRequest>empty()
    );
  }

  @Override
  public boolean isEnabled() {
    return spreadAllSlavesEnabled;
  }
}
