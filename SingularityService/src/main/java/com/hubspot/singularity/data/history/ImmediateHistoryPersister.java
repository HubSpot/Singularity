package com.hubspot.singularity.data.history;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityManagedCachedThreadPoolFactory;
import com.hubspot.singularity.SingularityManagedScheduledExecutorServiceFactory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.async.AsyncSemaphore;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.mesos.SingularitySchedulerLock;

@Singleton
public class ImmediateHistoryPersister {
  private static final Logger LOG = LoggerFactory.getLogger(ImmediateHistoryPersister.class);

  private final SingularityTaskHistoryPersister taskHistoryPersister;
  private final SingularityDeployHistoryPersister deployHistoryPersister;
  private final DeployManager deployManager;
  private final SingularitySchedulerLock lock;
  private final AsyncSemaphore<Void> immediatePersistSemaphore;
  private final ExecutorService immediatePersistExecutor;

  @Inject
  public ImmediateHistoryPersister(SingularityTaskHistoryPersister taskHistoryPersister,
                                   SingularityDeployHistoryPersister deployHistoryPersister,
                                   DeployManager deployManager,
                                   SingularityConfiguration configuration,
                                   SingularitySchedulerLock lock,
                                   SingularityManagedScheduledExecutorServiceFactory scheduledExecutorServiceFactory,
                                   SingularityManagedCachedThreadPoolFactory cachedThreadPoolFactory) {
    this.taskHistoryPersister = taskHistoryPersister;
    this.deployHistoryPersister = deployHistoryPersister;
    this.deployManager = deployManager;
    this.lock = lock;
    this.immediatePersistSemaphore = AsyncSemaphore.newBuilder(configuration::getMaxPendingImmediatePersists, scheduledExecutorServiceFactory.get("immediate-persist-sempahore", 1)).build();
    this.immediatePersistExecutor = cachedThreadPoolFactory.get("immediate-history-persist");
  }

  public void persistTaskAsync(SingularityTaskId taskId) {
    try {
      immediatePersistSemaphore.call(() ->
          CompletableFuture.runAsync(() ->
              lock.runWithRequestLock(() ->
                  taskHistoryPersister.moveToHistoryOrCheckForPurge(taskId, 0),
                  taskId.getRequestId(),
                  "immediate-task-history-persist"),
              immediatePersistExecutor)
      );
    } catch (Throwable t) {
      LOG.warn("Could not enqueue task {} for history persist, poller will persist later ({})", taskId, t);
    }
  }

  public void persistDeployAsync(String requestId, String deployId) {
    try {
      immediatePersistSemaphore.call(() ->
          CompletableFuture.runAsync(() ->
                  lock.runWithRequestLock(() -> {
                        Optional<SingularityDeployHistory> maybeDeployHistory = deployManager.getDeployHistory(requestId, deployId, true);
                        if (maybeDeployHistory.isPresent()) {
                          deployHistoryPersister.moveToHistoryOrCheckForPurge(maybeDeployHistory.get(), 0);
                        }
                      },
                      requestId,
                      "immediate-deploy-history-persist"),
              immediatePersistExecutor)
      );
    } catch (Throwable t) {
      LOG.warn("Could not enqueue deploy {} - {} for history persist, poller will persist later ({})", requestId, deployId, t);
    }
  }
}
