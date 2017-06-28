package com.hubspot.singularity.scheduler;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.google.common.math.DoubleMath;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityMainModule;

public class SingularityStatusUpdateDeltaPoller extends SingularityLeaderOnlyPoller {
  private final ConcurrentHashMap<Long, Long> statusUpdateDeltas;
  private final AtomicLong statusUpdateDelta30sAverage;

  @Inject
  public SingularityStatusUpdateDeltaPoller(@Named(SingularityMainModule.STATUS_UPDATE_DELTAS) ConcurrentHashMap<Long, Long> statusUpdateDeltas,
                                            @Named(SingularityMainModule.STATUS_UPDATE_DELTA_30S_AVERAGE) AtomicLong statusUpdateDelta30sAverage) {
    super(5L, TimeUnit.SECONDS);
    this.statusUpdateDeltas = statusUpdateDeltas;
    this.statusUpdateDelta30sAverage = statusUpdateDelta30sAverage;
  }

  @Override
  public void runActionOnPoll() {
    long now = System.currentTimeMillis();
    List<Long> toRemove = statusUpdateDeltas.keySet().stream()
        .filter((e) -> e < now - 30000)
        .collect(Collectors.toList());
    toRemove.forEach(statusUpdateDeltas::remove);
    if (statusUpdateDeltas.isEmpty()) {
      statusUpdateDelta30sAverage.set(0L);
    } else {
      statusUpdateDelta30sAverage.set((long) DoubleMath.mean(statusUpdateDeltas.values()));
    }
  }
}
