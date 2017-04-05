package com.hubspot.singularity.scheduler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;

@Singleton
public class SingularityLeaderCacheCoordinator {

  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final SingularityLeaderCache leaderCache;

  @Inject
  public SingularityLeaderCacheCoordinator(TaskManager taskManager, RequestManager requestManager, SingularityLeaderCache leaderCache) {
    this.taskManager = taskManager;
    this.requestManager = requestManager;
    this.leaderCache = leaderCache;
  }

  public void activateLeaderCache() {
    taskManager.activateLeaderCache();
    requestManager.activateLeaderCache();
    leaderCache.activate();
  }

  public void stopLeaderCache() {
    leaderCache.stop();
  }

}
