package com.hubspot.singularity.data.history;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityManagedCachedThreadPoolFactory;
import com.hubspot.singularity.SingularityManagedScheduledExecutorServiceFactory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.async.AsyncSemaphore;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.history.SingularityRequestHistoryPersister.SingularityRequestHistoryParent;
import com.hubspot.singularity.mesos.SingularitySchedulerLock;

@Singleton
public class ImmediateHistoryPersister {
  private static final Logger LOG = LoggerFactory.getLogger(ImmediateHistoryPersister.class);

  private final SingularityTaskHistoryPersister taskHistoryPersister;
  private final SingularityRequestHistoryPersister requestHistoryPersister;
  private final SingularityDeployHistoryPersister deployHistoryPersister;
  private final SingularitySchedulerLock lock;
  private final AsyncSemaphore<Void> immediatePersistSemaphore;
  private final ExecutorService immediatePersistExecutor;

  @Inject
  public ImmediateHistoryPersister(SingularityTaskHistoryPersister taskHistoryPersister,
                                   SingularityRequestHistoryPersister requestHistoryPersister,
                                   SingularityDeployHistoryPersister deployHistoryPersister,
                                   SingularityConfiguration configuration,
                                   SingularitySchedulerLock lock,
                                   SingularityManagedScheduledExecutorServiceFactory scheduledExecutorServiceFactory,
                                   SingularityManagedCachedThreadPoolFactory cachedThreadPoolFactory) {
    this.taskHistoryPersister = taskHistoryPersister;
    this.requestHistoryPersister = requestHistoryPersister;
    this.deployHistoryPersister = deployHistoryPersister;
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

  public void persistRequestAsync(SingularityRequestHistoryParent requestHistoryParent) {
    try {
      immediatePersistSemaphore.call(() ->
          CompletableFuture.runAsync(() ->
                  lock.runWithRequestLock(() ->
                      requestHistoryPersister.moveToHistoryOrCheckForPurge(requestHistoryParent, 0),
                      requestHistoryParent.getRequestId(),
                      "immediate-request-history-persist"),
              immediatePersistExecutor)
      );
    } catch (Throwable t) {
      LOG.warn("Could not enqueue task {} for history persist, poller will persist later ({})", requestHistoryParent.getRequestId(), t);
    }
  }

  public void persistDeployAsync(SingularityDeployHistory deployHistory) {
    try {
      immediatePersistSemaphore.call(() ->
          CompletableFuture.runAsync(() ->
                  lock.runWithRequestLock(() ->
                      deployHistoryPersister.moveToHistoryOrCheckForPurge(deployHistory, 0),
                      deployHistory.getDeployMarker().getRequestId(),
                      "immediate-deploy-history-persist"),
              immediatePersistExecutor)
      );
    } catch (Throwable t) {
      LOG.warn("Could not enqueue task {} for history persist, poller will persist later ({})", deployHistory.getDeployMarker(), t);
    }
  }
}
