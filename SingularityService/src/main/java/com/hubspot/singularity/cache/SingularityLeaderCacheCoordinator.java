package com.hubspot.singularity.cache;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RackManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.usage.UsageManager;

@Singleton
public class SingularityLeaderCacheCoordinator {

  private final TaskManager taskManager;
  private final DeployManager deployManager;
  private final RequestManager requestManager;
  private final SlaveManager slaveManager;
  private final RackManager rackManager;
  private final UsageManager usageManager;
  private final SingularityCache cache;

  @Inject
  public SingularityLeaderCacheCoordinator(TaskManager taskManager,
                                           DeployManager deployManager,
                                           RequestManager requestManager,
                                           SlaveManager slaveManager,
                                           RackManager rackManager,
                                           UsageManager usageManager,
                                           SingularityCache cache) {
    this.taskManager = taskManager;
    this.deployManager = deployManager;
    this.requestManager = requestManager;
    this.slaveManager = slaveManager;
    this.rackManager = rackManager;
    this.usageManager = usageManager;
    this.cache = cache;
  }

  public void activateLeaderCache() {
    taskManager.activateLeaderCache();
    deployManager.activateLeaderCache();
    requestManager.activateLeaderCache();
    slaveManager.activateLeaderCache();
    rackManager.activateLeaderCache();
    usageManager.activateLeaderCache();
    cache.markLeader();
  }

  public void stopLeaderCache() {
    cache.markNotLeader();
  }

}
