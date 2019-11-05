package com.hubspot.singularity.scheduler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.async.CompletableFutures;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RackManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.usage.UsageManager;

@Singleton
public class SingularityLeaderCacheCoordinator {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityLeaderCacheCoordinator.class);

  private final TaskManager taskManager;
  private final DeployManager deployManager;
  private final RequestManager requestManager;
  private final SlaveManager slaveManager;
  private final RackManager rackManager;
  private final UsageManager usageManager;
  private final SingularityLeaderCache leaderCache;

  @Inject
  public SingularityLeaderCacheCoordinator(TaskManager taskManager,
                                           DeployManager deployManager,
                                           RequestManager requestManager,
                                           SlaveManager slaveManager,
                                           RackManager rackManager,
                                           UsageManager usageManager,
                                           SingularityLeaderCache leaderCache) {
    this.taskManager = taskManager;
    this.deployManager = deployManager;
    this.requestManager = requestManager;
    this.slaveManager = slaveManager;
    this.rackManager = rackManager;
    this.usageManager = usageManager;
    this.leaderCache = leaderCache;
  }

  public void activateLeaderCache() {
    long start = System.currentTimeMillis();
    ExecutorService leaderCacheExecutor = Executors.newFixedThreadPool(6, new ThreadFactoryBuilder().setNameFormat("leader-cache-%d").build());
    CompletableFutures.allOf(
        ImmutableList.of(
            CompletableFuture.runAsync(taskManager::activateLeaderCache, leaderCacheExecutor),
            CompletableFuture.runAsync(deployManager::activateLeaderCache, leaderCacheExecutor),
            CompletableFuture.runAsync(requestManager::activateLeaderCache, leaderCacheExecutor),
            CompletableFuture.runAsync(slaveManager::activateLeaderCache, leaderCacheExecutor),
            CompletableFuture.runAsync(rackManager::activateLeaderCache, leaderCacheExecutor),
            CompletableFuture.runAsync(usageManager::activateLeaderCache, leaderCacheExecutor)
        )
    ).join();
    leaderCache.activate();
    try {
      leaderCacheExecutor.shutdown();
    } catch (Throwable t) {
      LOG.error("Unable to properly shut down leader cache executor", t);
    }
    LOG.info("Populated leader cache after {}ms", System.currentTimeMillis() - start);
  }

  public void stopLeaderCache() {
    leaderCache.stop();
  }

  public void clear() {
    leaderCache.clear();
  }
}
