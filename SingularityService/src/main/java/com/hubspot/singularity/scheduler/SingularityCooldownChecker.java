package com.hubspot.singularity.scheduler;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityManagedThreadPoolFactory;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.async.CompletableFutures;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.mesos.SingularitySchedulerLock;

@Singleton
public class SingularityCooldownChecker {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityCooldownChecker.class);

  private final RequestManager requestManager;
  private final DeployManager deployManager;
  private final SingularityCooldown cooldown;
  private final SingularitySchedulerLock lock;
  private final ExecutorService cooldownExecutor;

  @Inject
  public SingularityCooldownChecker(RequestManager requestManager, DeployManager deployManager, SingularityCooldown cooldown, SingularitySchedulerLock lock,
                                    SingularityManagedThreadPoolFactory threadPoolFactory, SingularityConfiguration configuration) {
    this.requestManager = requestManager;
    this.deployManager = deployManager;
    this.cooldown = cooldown;
    this.lock = lock;
    this.cooldownExecutor = threadPoolFactory.get("cooldown-checker", configuration.getCoreThreadpoolSize());
  }

  public void checkCooldowns() {
    final long start = System.currentTimeMillis();

    final List<SingularityRequestWithState> cooldownRequests = Lists.newArrayList(requestManager.getCooldownRequests(false));

    if (cooldownRequests.isEmpty()) {
      LOG.trace("No cooldown requests");
      return;
    }

    AtomicInteger exitedCooldown = new AtomicInteger(0);

    CompletableFutures.allOf(
        cooldownRequests.stream()
            .map((cooldownRequest) ->
                CompletableFuture.runAsync(
                    () ->
                        lock.runWithRequestLock(() -> {
                          if (checkCooldown(cooldownRequest)) {
                            exitedCooldown.getAndIncrement();
                          }
                        }, cooldownRequest.getRequest().getId(), getClass().getSimpleName()),
                    cooldownExecutor))
            .collect(Collectors.toList())
    ).join();

    LOG.info("{} out of {} cooldown requests exited cooldown in {}", exitedCooldown.get(), cooldownRequests.size(), JavaUtils.duration(start));
  }

  private boolean checkCooldown(SingularityRequestWithState cooldownRequest) {
    if (shouldExitCooldown(cooldownRequest)) {
      requestManager.exitCooldown(cooldownRequest.getRequest(), System.currentTimeMillis(), Optional.<String>empty(), Optional.<String>empty());
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

    return cooldown.hasCooldownExpired(maybeDeployStatistics.get(), Optional.empty());
  }

}
