package com.hubspot.singularity.scheduler;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.config.SingularityConfiguration;

public class SingularityCooldown {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityCooldown.class);

  private final SingularityConfiguration configuration;

  @Inject
  public SingularityCooldown(SingularityConfiguration configuration) {
    this.configuration = configuration;
  }

  public boolean hasCooldownExpired(SingularityDeployStatistics deployStatistics, Optional<Long> recentFailureTimestamp) {
    if (configuration.getCooldownExpiresAfterMinutes() < 1) {
      return true;
    }

    int numberOfFailuresInsideExpiration = 0;

    for (long failureTimestamp : deployStatistics.getSequentialFailureTimestamps()) {
      if (hasFailedInsideCooldown(failureTimestamp)) {
        numberOfFailuresInsideExpiration++;
      }
    }

    if (recentFailureTimestamp.isPresent() && hasFailedInsideCooldown(recentFailureTimestamp.get())) {
      numberOfFailuresInsideExpiration++;
    }

    final boolean hasCooldownExpired = numberOfFailuresInsideExpiration < configuration.getCooldownAfterFailures();

    if (hasCooldownExpired) {
      LOG.trace("Request {} cooldown has expired or is not valid because only {} (required: {}) tasks have failed in the last {}", deployStatistics.getRequestId(), numberOfFailuresInsideExpiration, configuration.getCooldownAfterFailures(), JavaUtils.durationFromMillis(TimeUnit.MINUTES.toMillis(configuration.getCooldownExpiresAfterMinutes())));
    }

    return hasCooldownExpired;
  }

  private boolean hasFailedInsideCooldown(long failureTimestamp) {
    final long timeSinceFailure = System.currentTimeMillis() - failureTimestamp;

    return timeSinceFailure < TimeUnit.MINUTES.toMillis(configuration.getCooldownExpiresAfterMinutes());
  }

}
