package com.hubspot.singularity.scheduler;

import java.util.List;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;

@Singleton
public class SingularityCooldownChecker {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityCooldownChecker.class);

  private final RequestManager requestManager;
  private final DeployManager deployManager;

  private final SingularityCooldown cooldown;

  @Inject
  public SingularityCooldownChecker(RequestManager requestManager, DeployManager deployManager, SingularityCooldown cooldown) {
    this.requestManager = requestManager;
    this.deployManager = deployManager;
    this.cooldown = cooldown;
  }

  public void checkCooldowns() {
    final long start = System.currentTimeMillis();

    final List<SingularityRequestWithState> cooldownRequests = Lists.newArrayList(requestManager.getCooldownRequests(false));

    if (cooldownRequests.isEmpty()) {
      LOG.trace("No cooldown requests");
      return;
    }

    int exitedCooldown = 0;

    for (SingularityRequestWithState cooldownRequest : cooldownRequests) {
      if (checkCooldown(cooldownRequest)) {
        exitedCooldown++;
      }
    }

    LOG.info("{} out of {} cooldown requests exited cooldown in {}", exitedCooldown, cooldownRequests.size(), JavaUtils.duration(start));
  }

  private boolean checkCooldown(SingularityRequestWithState cooldownRequest) {
    if (shouldExitCooldown(cooldownRequest)) {
      requestManager.exitCooldown(cooldownRequest.getRequest(), System.currentTimeMillis(), Optional.<String> absent(), Optional.<String> absent());
      return true;
    }

    return false;
  }

  private boolean shouldExitCooldown(SingularityRequestWithState cooldownRequest) {
    Optional<SingularityRequestDeployState> maybeDeployState = deployManager.getRequestDeployState(cooldownRequest.getRequest().getId());

    if (!maybeDeployState.isPresent() || !maybeDeployState.get().getActiveDeploy().isPresent()) {
      LOG.trace("{} had no deployState / activeDeploy {}, exiting cooldown", cooldownRequest.getRequest().getId(), maybeDeployState);
      return true;
    }

    Optional<SingularityDeployStatistics> maybeDeployStatistics = deployManager.getDeployStatistics(cooldownRequest.getRequest().getId(), maybeDeployState.get().getActiveDeploy().get().getDeployId());

    if (!maybeDeployStatistics.isPresent()) {
      LOG.trace("{} had no deploy statistics, exiting cooldown", new SingularityDeployKey(cooldownRequest.getRequest().getId(), maybeDeployState.get().getActiveDeploy().get().getDeployId()));
      return true;
    }

    Optional<Long> lastFinishAt = maybeDeployStatistics.get().getLastFinishAt();

    if (!lastFinishAt.isPresent()) {
      LOG.trace("{} had no last finish, exiting cooldown", new SingularityDeployKey(cooldownRequest.getRequest().getId(), maybeDeployState.get().getActiveDeploy().get().getDeployId()));
      return true;
    }

    if (cooldown.hasCooldownExpired(cooldownRequest.getRequest(), maybeDeployStatistics.get(), Optional.<Integer> absent(), Optional.<Long> absent())) {
      return true;
    }

    return false;
  }

}
