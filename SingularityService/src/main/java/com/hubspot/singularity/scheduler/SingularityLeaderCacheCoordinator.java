package com.hubspot.singularity.scheduler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.data.TaskManager;

@Singleton
public class SingularityLeaderCacheCoordinator {

  private final TaskManager taskManager;
  private final SingularityLeaderCache leaderCache;

  @Inject
  public SingularityLeaderCacheCoordinator(TaskManager taskManager, SingularityLeaderCache leaderCache) {
    this.taskManager = taskManager;
    this.leaderCache = leaderCache;
  }

  public void activateLeaderCache() {
    leaderCache.activate();
    taskManager.activateLeaderCache();
  }

  public void stopLeaderCache() {
    leaderCache.stop();
  }

}
