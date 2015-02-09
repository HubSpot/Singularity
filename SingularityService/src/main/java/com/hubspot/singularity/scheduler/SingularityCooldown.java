package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SingularityConfiguration;

@Singleton
public class SingularityCooldown {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityCooldown.class);

  private final SingularityConfiguration configuration;
  private final long cooldownExpiresAfterMillis;

  @Inject
  public SingularityCooldown(SingularityConfiguration configuration) {
    this.configuration = configuration;
    this.cooldownExpiresAfterMillis = TimeUnit.MINUTES.toMillis(configuration.getCooldownExpiresAfterMinutes());
  }

  public boolean shouldEnterCooldown(SingularityRequest request, SingularityTaskId taskId, RequestState requestState, SingularityDeployStatistics deployStatistics, long failureTimestamp) {
    if (requestState != RequestState.ACTIVE || !request.isAlwaysRunning()) {
      return false;
    }

    if (configuration.getCooldownAfterFailures() < 1 || configuration.getCooldownExpiresAfterMinutes() < 1) {
      return false;
    }

    final boolean failedTooManyTimes = hasFailedTooManyTimes(request, deployStatistics, Optional.of(taskId.getInstanceNo()), Optional.of(failureTimestamp));

    if (failedTooManyTimes) {
      LOG.trace("Request {} has failed at least {} times in {}", request.getId(), configuration.getCooldownAfterFailures(), configuration.getCooldownAfterFailures());
    }

    return failedTooManyTimes;
  }

  private boolean hasFailedTooManyTimes(SingularityRequest request, SingularityDeployStatistics deployStatistics, Optional<Integer> instanceNo, Optional<Long> recentFailureTimestamp) {
    final long now = System.currentTimeMillis();

    int numInstancesThatMustFail = (int) Math.ceil(request.getInstancesSafe() * configuration.getCooldownAfterPctOfInstancesFail());
    int numInstancesThatFailed = 0;

    for (int i = 1; i < request.getInstancesSafe() + 1; i++) {
      int numFailuresInsideCooldown = 0;

      for (long failureTimestamp : deployStatistics.getInstanceSequentialFailureTimestamps().get(i)) {
        if (hasFailedInsideCooldown(now, failureTimestamp)) {
          numFailuresInsideCooldown++;
        }
      }

      if (instanceNo.isPresent() && instanceNo.get() == i && recentFailureTimestamp.isPresent()) {
        if (hasFailedInsideCooldown(now, recentFailureTimestamp.get())) {
          numFailuresInsideCooldown++;
        }
      }

      if (numFailuresInsideCooldown >= configuration.getCooldownAfterFailures()) {
        numInstancesThatFailed++;
      }
    }

    return numInstancesThatFailed >= numInstancesThatMustFail;
  }

  public boolean hasCooldownExpired(SingularityRequest request, SingularityDeployStatistics deployStatistics, Optional<Integer> instanceNo, Optional<Long> recentFailureTimestamp) {
    if (configuration.getCooldownExpiresAfterMinutes() < 1) {
      return true;
    }

    final boolean hasCooldownExpired = !hasFailedTooManyTimes(request, deployStatistics, instanceNo, recentFailureTimestamp);

    if (hasCooldownExpired) {
      LOG.trace("Request {} cooldown has expired or is not valid because {} tasks have not failed in the last {}", deployStatistics.getRequestId(), configuration.getCooldownAfterFailures(), JavaUtils.durationFromMillis(cooldownExpiresAfterMillis));
    }

    return hasCooldownExpired;
  }

  private boolean hasFailedInsideCooldown(long now, long failureTimestamp) {
    final long timeSinceFailure = now - failureTimestamp;

    return timeSinceFailure < cooldownExpiresAfterMillis;
  }

}
