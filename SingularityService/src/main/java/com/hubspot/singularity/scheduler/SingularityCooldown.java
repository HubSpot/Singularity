package com.hubspot.singularity.scheduler;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.config.SingularityConfiguration;

@Singleton
public class SingularityCooldown {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityCooldown.class);

  private final SingularityConfiguration configuration;

  @Inject
  public SingularityCooldown(SingularityConfiguration configuration) {
    this.configuration = configuration;
  }

  boolean shouldEnterCooldown(SingularityRequest request, RequestState requestState, SingularityDeployStatistics deployStatistics, long failureTimestamp) {
    if (requestState != RequestState.ACTIVE || !request.isAlwaysRunning()) {
      return false;
    }

    return hasFailedTooManyTimes(deployStatistics, Optional.of(failureTimestamp));
  }

  private boolean hasFailedTooManyTimes(SingularityDeployStatistics deployStatistics, Optional<Long> recentFailureTimestamp) {
    return hasFastFailureLoop(deployStatistics, recentFailureTimestamp) || hasSlowFailureLoop(deployStatistics, recentFailureTimestamp);
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
    return !hasFailedTooManyTimes(deployStatistics, recentFailureTimestamp);
  }
}
