package com.hubspot.singularity.scheduler;

import java.util.Collection;
import java.util.Comparator;
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
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityRequest;
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
    // TODO - fast failure loops only
    return false;
  }

  private boolean hasSlowFailureLoop(SingularityDeployStatistics deployStatistics, Optional<Long> recentFailureTimestamp) {
    return hasFailureLoop(deployStatistics, recentFailureTimestamp, configuration.getSlowFailureCooldownMs(), configuration.getSlowFailureCooldownCount(), configuration.getSlowCooldownExpiresMinutesWithoutFailure());
  }

  private boolean hasFastFailureLoop(SingularityDeployStatistics deployStatistics, Optional<Long> recentFailureTimestamp) {
    return hasFailureLoop(deployStatistics, recentFailureTimestamp, configuration.getFastFailureCooldownMs(), configuration.getFastFailureCooldownCount(), configuration.getFastCooldownExpiresMinutesWithoutFailure());

  }

  private boolean hasFailureLoop(SingularityDeployStatistics deployStatistics, Optional<Long> recentFailureTimestamp, long cooldownPeriod, int cooldownCount, long expiresAfterMins) {
    final long now = System.currentTimeMillis();
    long thresholdTime = now - cooldownPeriod;
    List<Long> failureTimestamps = deployStatistics.getInstanceSequentialFailureTimestamps().asMap()
        .values()
        .stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
    if (recentFailureTimestamp.isPresent()) {
      failureTimestamps.add(recentFailureTimestamp.get());
    }
    long failureCount = failureTimestamps.stream()
        .filter((t) -> t > thresholdTime)
        .count();
    java.util.Optional<Long> mostRecentFailure = failureTimestamps.stream().max(Comparator.comparingLong(Long::valueOf));

    boolean mostRecentFailureOutsideWindow = !mostRecentFailure.isPresent() || mostRecentFailure.get() < System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(expiresAfterMins);

    return failureCount >= cooldownCount && !mostRecentFailureOutsideWindow;
  }

  boolean hasCooldownExpired(SingularityDeployStatistics deployStatistics, Optional<Long> recentFailureTimestamp) {
    return !shouldBeInCooldown(deployStatistics, recentFailureTimestamp);
  }

  Set<CrashLoopInfo> getActiveCrashLoops(SingularityDeployStatistics deployStatistics) {
    // TODO
    return null;
  }
}
