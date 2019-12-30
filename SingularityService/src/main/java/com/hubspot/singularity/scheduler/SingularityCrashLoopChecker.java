package com.hubspot.singularity.scheduler;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import com.hubspot.singularity.CrashLoopInfo;
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
public class SingularityCrashLoopChecker {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityCrashLoopChecker.class);

  private final RequestManager requestManager;
  private final DeployManager deployManager;
  private final SingularityCrashLoops crashLoops;
  private final SingularitySchedulerLock lock;
  private final ExecutorService cooldownExecutor;

  @Inject
  public SingularityCrashLoopChecker(RequestManager requestManager, DeployManager deployManager, SingularityCrashLoops crashLoops, SingularitySchedulerLock lock,
                                     SingularityManagedThreadPoolFactory threadPoolFactory, SingularityConfiguration configuration) {
    this.requestManager = requestManager;
    this.deployManager = deployManager;
    this.crashLoops = crashLoops;
    this.lock = lock;
    this.cooldownExecutor = threadPoolFactory.get("crash-loop-checker", configuration.getCoreThreadpoolSize());
  }

  public void checkCooldowns() {
    final long start = System.currentTimeMillis();

    // cooldown reserved for fast loop, check crash loops separately
    final List<SingularityRequestWithState> cooldownRequests = Lists.newArrayList(requestManager.getCooldownRequests(false));

    AtomicInteger exitedCooldown = new AtomicInteger(0);
    Map<SingularityDeployKey, Optional<SingularityDeployStatistics>> deployStatsCache = new HashMap<>();

    if (!cooldownRequests.isEmpty()) {
      CompletableFutures.allOf(
          cooldownRequests.stream()
              .map((cooldownRequest) ->
                  CompletableFuture.runAsync(
                      () ->
                          lock.runWithRequestLock(() -> {
                            if (checkCooldown(cooldownRequest, deployStatsCache)) {
                              exitedCooldown.getAndIncrement();
                            }
                          }, cooldownRequest.getRequest().getId(), getClass().getSimpleName()),
                      cooldownExecutor))
              .collect(Collectors.toList())
      ).join();
    }

    // Check for crash loops
    for (SingularityRequestWithState request : requestManager.getActiveRequests()) {
      Optional<SingularityRequestDeployState> maybeDeployState = deployManager.getRequestDeployState(request.getRequest().getId());

      if (!maybeDeployState.isPresent() || !maybeDeployState.get().getActiveDeploy().isPresent()) {
        continue;
      }
      // Remove outdated loops on new deploy
      List<CrashLoopInfo> previouslyActive = requestManager.getCrashLoopsForRequest(request.getRequest().getId())
          .stream()
          .filter((l) -> {
            if (!l.getDeployId().equals(maybeDeployState.get().getActiveDeploy().get().getDeployId())) {
              requestManager.deleteCrashLoop(l);
              return false;
            }
            return true;
          })
          .filter((l) -> !l.getEnd().isPresent())
          .collect(Collectors.toList());

      Optional<SingularityDeployStatistics> maybeDeployStatistics = deployStatsCache.computeIfAbsent(
          new SingularityDeployKey(request.getRequest().getId(), maybeDeployState.get().getActiveDeploy().get().getDeployId()),
          (i) -> deployManager.getDeployStatistics(request.getRequest().getId(), maybeDeployState.get().getActiveDeploy().get().getDeployId())
      );
      if (!maybeDeployStatistics.isPresent()) {
        continue;
      }

      Set<CrashLoopInfo> active = crashLoops.getActiveCrashLoops(maybeDeployStatistics.get());

      if (!active.isEmpty()) {
        active.forEach((l) -> {
          if (!previouslyActive.contains(l)) {
            LOG.info("New crash loop for {}: {}", request.getRequest().getId(), l);
            requestManager.saveCrashLoop(l);
          }
        });
      }

      if (!previouslyActive.isEmpty()) {
        previouslyActive.forEach((l) -> {
          if (!active.contains(l)) {
            LOG.info("Crash loop resolved for {}: {}", request.getRequest().getId(), l);
            requestManager.saveCrashLoop(new CrashLoopInfo(l.getRequestId(), l.getDeployId(), l.getStart(), Optional.of(System.currentTimeMillis()), l.getType()));
          }
        });
      }

      // Only keep the most recent 20 crash loop infos
      previouslyActive.stream()
          .filter((l) -> l.getEnd().isPresent())
          .sorted(Comparator.comparingLong(CrashLoopInfo::getStart).reversed())
          .skip(10)
          .forEach(requestManager::deleteCrashLoop);
    }

    LOG.info("{} out of {} cooldown requests exited cooldown in {}", exitedCooldown.get(), cooldownRequests.size(), JavaUtils.duration(start));
  }

  private boolean checkCooldown(SingularityRequestWithState cooldownRequest, Map<SingularityDeployKey, Optional<SingularityDeployStatistics>> deployStatsCache) {
    if (shouldExitCooldown(cooldownRequest, deployStatsCache)) {
      requestManager.exitCooldown(cooldownRequest.getRequest(), System.currentTimeMillis(), Optional.<String>empty(), Optional.<String>empty());
      return true;
    }

    return false;
  }

  private boolean shouldExitCooldown(SingularityRequestWithState cooldownRequest, Map<SingularityDeployKey, Optional<SingularityDeployStatistics>> deployStatsCache) {
    Optional<SingularityRequestDeployState> maybeDeployState = deployManager.getRequestDeployState(cooldownRequest.getRequest().getId());

    if (!maybeDeployState.isPresent() || !maybeDeployState.get().getActiveDeploy().isPresent()) {
      LOG.trace("{} had no deployState / activeDeploy {}, exiting cooldown", cooldownRequest.getRequest().getId(), maybeDeployState);
      return true;
    }

    Optional<SingularityDeployStatistics> maybeDeployStatistics = deployStatsCache.computeIfAbsent(
        new SingularityDeployKey(cooldownRequest.getRequest().getId(), maybeDeployState.get().getActiveDeploy().get().getDeployId()),
        (i) -> deployManager.getDeployStatistics(cooldownRequest.getRequest().getId(), maybeDeployState.get().getActiveDeploy().get().getDeployId())
    );

    if (!maybeDeployStatistics.isPresent()) {
      LOG.trace("{} had no deploy statistics, exiting cooldown", new SingularityDeployKey(cooldownRequest.getRequest().getId(), maybeDeployState.get().getActiveDeploy().get().getDeployId()));
      return true;
    }

    Optional<Long> lastFinishAt = maybeDeployStatistics.get().getLastFinishAt();

    if (!lastFinishAt.isPresent()) {
      LOG.trace("{} had no last finish, exiting cooldown", new SingularityDeployKey(cooldownRequest.getRequest().getId(), maybeDeployState.get().getActiveDeploy().get().getDeployId()));
      return true;
    }

    return crashLoops.hasCooldownExpired(maybeDeployStatistics.get(), Optional.empty());
  }
}
