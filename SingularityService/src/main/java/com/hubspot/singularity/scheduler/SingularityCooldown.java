package com.hubspot.singularity.scheduler;

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

  public boolean hasCooldownExpired(SingularityDeployStatistics deployStatistics) {
    if (configuration.getCooldownExpiresAfterMinutes() < 1 || !deployStatistics.getLastFinishAt().isPresent()) {
      return false;
    }

    final long cooldownExpiresMillis = TimeUnit.MINUTES.toMillis(configuration.getCooldownExpiresAfterMinutes());

    final long lastFinishAt = deployStatistics.getLastFinishAt().get().longValue();
    final long timeSinceLastFinish = System.currentTimeMillis() - lastFinishAt;

    final boolean hasCooldownExpired = timeSinceLastFinish > cooldownExpiresMillis;

    if (hasCooldownExpired) {
      LOG.trace("Request {} cooldown has expired or is not valid because the last task finished {} ago (cooldowns expire after {})", deployStatistics.getRequestId(), JavaUtils.durationFromMillis(timeSinceLastFinish), JavaUtils.durationFromMillis(cooldownExpiresMillis));
    }

    return hasCooldownExpired;
  }
}
