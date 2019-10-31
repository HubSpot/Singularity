package com.hubspot.singularity.scheduler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.google.common.math.Stats;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.config.SingularityConfiguration;

public class SingularityStatusUpdateDeltaPoller extends SingularityLeaderOnlyPoller {
  private final SingularityConfiguration configuration;
  private final Map<String, Map<Long, Long>> statusUpdateDeltas;
  private final AtomicBoolean shortCircuitForStatusUpdateDelay;

  @Inject
  public SingularityStatusUpdateDeltaPoller(SingularityConfiguration configuration,
                                            @Named(SingularityMainModule.STATUS_UPDATE_DELTAS) Map<String, Map<Long, Long>> statusUpdateDeltas,
                                            @Named(SingularityMainModule.STATUS_UPDATE_SHORT_CIRCUIT) AtomicBoolean shortCircuitForStatusUpdateDelay) {
    super(5L, TimeUnit.SECONDS);
    this.configuration = configuration;
    this.statusUpdateDeltas = statusUpdateDeltas;
    this.shortCircuitForStatusUpdateDelay = shortCircuitForStatusUpdateDelay;
  }

  @Override
  public void runActionOnPoll() {
    long now = System.currentTimeMillis();
    Set<String> requestIds = statusUpdateDeltas.keySet();
    int overThreshold = 0;
    for (String requestId : requestIds) {
      Map<Long, Long> requestDeltas = statusUpdateDeltas.get(requestId);
      List<Long> toRemove = requestDeltas.keySet()
          .stream()
          .filter((e) -> e < now - 30000)
          .collect(Collectors.toList());
      toRemove.forEach(requestDeltas::remove);
      if (requestDeltas.size() > 0 && Stats.meanOf(requestDeltas.values()) > configuration.getDelayPollersWhenDeltaOverMs()) {
        overThreshold++;
      }
    }

    if ((overThreshold * 100.0 / requestIds.size()) > configuration.getDelayPollersWhenPercentOfRequestsOverUpdateDelta()) {
      shortCircuitForStatusUpdateDelay.set(true);
    } else {
      shortCircuitForStatusUpdateDelay.set(false);
    }
  }
}
