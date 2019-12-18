package com.hubspot.singularity.scheduler;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.singularity.CrashLoopInfo;
import com.hubspot.singularity.CrashLoopType;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.TaskFailureEvent;
import com.hubspot.singularity.config.SingularityConfiguration;

@Singleton
public class SingularityCrashLoops {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityCrashLoops.class);

  private final SingularityConfiguration configuration;

  @Inject
  public SingularityCrashLoops(SingularityConfiguration configuration) {
    this.configuration = configuration;
  }

  boolean shouldEnterCooldown(SingularityRequest request, RequestState requestState, SingularityDeployStatistics deployStatistics, long failureTimestamp) {
    if (requestState != RequestState.ACTIVE || !request.isAlwaysRunning()) {
      return false;
    }

    return shouldBeInCooldown(deployStatistics, Optional.of(failureTimestamp));
  }

  private boolean shouldBeInCooldown(SingularityDeployStatistics deployStatistics, Optional<Long> recentFailureTimestamp) {
    return cooldownStart(deployStatistics, recentFailureTimestamp).isPresent();
  }

  private Optional<Long> cooldownStart(SingularityDeployStatistics deployStatistics, Optional<Long> recentFailureTimestamp) {
    long threshold = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1);
    List<Long> failureTimestamps = deployStatistics.getTaskFailureEvents().stream()
        .map(TaskFailureEvent::getTimestamp)
        .collect(Collectors.toList());;
    recentFailureTimestamp.ifPresent(failureTimestamps::add);
    List<Long> pastThreshold = failureTimestamps.stream()
        .filter((t) -> t > threshold)
        .collect(Collectors.toList());
    if (pastThreshold.size() > 5) {
      return pastThreshold.stream().max(Comparator.comparingLong(Long::longValue));
    }
    return Optional.empty();
  }

  boolean hasCooldownExpired(SingularityDeployStatistics deployStatistics, Optional<Long> recentFailureTimestamp) {
    return !shouldBeInCooldown(deployStatistics, recentFailureTimestamp);
  }

  Set<CrashLoopInfo> getActiveCrashLoops(SingularityDeployStatistics deployStatistics) {
    Set<CrashLoopInfo> active = new HashSet<>();

    // Check fast failures
    cooldownStart(deployStatistics, Optional.empty())
        .ifPresent((start) ->
            active.add(new CrashLoopInfo(
                deployStatistics.getRequestId(),
                deployStatistics.getDeployId(),
                start,
                Optional.empty(),
                CrashLoopType.FAST_FAILURE_LOOP
            )));

    /*
     * Startup failure loop
     * a) multiple instances failing healthchecks, or single instance failing healthchecks too many times in X minutes
     * b) small count of failures (3?) but instance no matches one that is in cleaning state waiting for a replacement
     */

    /*
     * OOM Danger. > X OOMs in Y minutes across all instances
     */

    /*
     * Single instance failure. > X failures with same instance no in X minutes
     */

    /*
     * Multi instance failure. > X% of instances failing within Y minutes
     */

    /*
     * Unexpected Exits. Too many task finished from a long-running type in X minutes
     */

    /*
     * Slow failures. Occasional failures, count on order of hours, looking for consistency in non-zero count each hour
     */


    return active;
  }
}
