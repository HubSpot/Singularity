package com.hubspot.singularity.mesos;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityManagedThreadPoolFactory;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.mesos.SingularityOfferCache.CachedOffer;
import io.swagger.v3.oas.models.security.SecurityScheme.In;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.mesos.v1.Protos.Offer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SingularityPendingTaskQueueProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityPendingTaskQueueProcessor.class
  );

  private static final int PARALLEL_LOCK_WAIT = 15;
  private static final String LOCK_NAME = "pending-task-queue";

  private final LeaderLatch leaderLatch;
  private final SingularityOfferCache offerCache;
  private final PriorityBlockingQueue<SingularityPendingTask> preLockQueue;
  private final PriorityBlockingQueue<SingularityPendingTask> pendingTaskQueue;
  private final ExecutorService lockExecutor;
  private final ExecutorService queueExecutor;
  private final AtomicBoolean running;
  private final SingularitySchedulerMetrics metrics;
  private final SingularitySchedulerLock lock;
  private final Map<String, Long> lockStarts;

  private Future<?> queueFuture = null;

  @Inject
  public SingularityPendingTaskQueueProcessor(
    LeaderLatch leaderLatch,
    SingularityOfferCache offerCache,
    SingularityManagedThreadPoolFactory threadPoolFactory,
    SingularitySchedulerMetrics metrics,
    SingularitySchedulerLock lock
  ) {
    this.leaderLatch = leaderLatch;
    this.offerCache = offerCache;
    this.pendingTaskQueue = new PriorityBlockingQueue<>();
    this.preLockQueue = new PriorityBlockingQueue<>();
    this.queueExecutor = threadPoolFactory.get("pending-task-queue", 2);
    this.lockExecutor =
      threadPoolFactory.get("pending-task-lock-wait", PARALLEL_LOCK_WAIT);
    this.running = new AtomicBoolean(false);
    this.metrics = metrics;
    this.lock = lock;
    this.lockStarts = new ConcurrentHashMap<>();
  }

  public void start() {
    if (leaderLatch.hasLeadership()) {
      if (queueFuture != null) {
        queueFuture.cancel(true);
      }
      running.set(true);
      queueFuture = queueExecutor.submit(this::run);
    }
  }

  public void addPendingTask(SingularityPendingTask pendingTask) {
    preLockQueue.put(pendingTask);
  }

  public void removePendingTask(SingularityPendingTaskId toRemove) {
    if (!preLockQueue.remove(toRemove)) {
      if (!pendingTaskQueue.remove(toRemove)) {
        LOG.warn("Unable to remove pending task {}", toRemove.getId());
      }
    }
  }

  public void stop() {
    running.set(false);
    if (queueFuture != null) {
      queueFuture.cancel(false);
    }
  }

  private void runLockWait() {
    Map<String, CompletableFuture<Void>> waitFutures = new HashMap<>();
    while (running.get()) {
      long start = System.currentTimeMillis();
      try {
        if (waitFutures.size() < PARALLEL_LOCK_WAIT) {
          SingularityPendingTask pendingTask = preLockQueue.poll();
          waitFutures.put(
            pendingTask.getPendingTaskId().getId(),
            CompletableFuture.runAsync(
              () -> waitLockAndEnqueue(pendingTask),
              lockExecutor
            )
          );
        }
      } catch (Exception e) {
        LOG.error("Error running task launch", e);
      }
    }
  }

  private void waitLockAndEnqueue(SingularityPendingTask pendingTask) {
    try {
      lockStarts.put(
        pendingTask.getPendingTaskId().getId(),
        lock.lock(pendingTask.getPendingTaskId().getRequestId(), LOCK_NAME)
      );
      pendingTaskQueue.put(pendingTask);
    } catch (Exception e) {
      LOG.error(
        "Could not acquire lock for task {}",
        pendingTask.getPendingTaskId().getId(),
        e
      );
      lock.unlock(
        pendingTask.getPendingTaskId().getRequestId(),
        LOCK_NAME,
        Optional
          .ofNullable(lockStarts.remove(pendingTask.getPendingTaskId().getId()))
          .orElse(System.currentTimeMillis())
      );
    }
  }

  // Everything in this method is run behind a request-level lock and offer lock
  private void run() {
    while (running.get()) {
      long start = System.currentTimeMillis();
      SingularityPendingTask toLaunch = pendingTaskQueue.poll();
      try {
        // Grab offers grouped by agent
        Map<String, SingularityOfferHolder> offers = null; // TODO
        SingularityOfferHolder bestOffer = null;
        double maxOfferScore = 0.0;
        for (Map.Entry<String, SingularityOfferHolder> entry : offers.entrySet()) {
          double offerScore = scoreOffer(toLaunch, entry.getValue());
          if (offerScore > maxOfferScore) {
            bestOffer = entry.getValue();
          }
          // TODO - if max score 'good enough' break
        }

        if (bestOffer != null) {
          // Launch task
        } else {
          // Back on the queue and try again. Temporarily free up lock for other pollers first
          preLockQueue.put(toLaunch);
        }

        metrics.getOfferLoopTime().update(System.currentTimeMillis() - start);
      } catch (Exception e) {
        LOG.error("Error running task launch", e);
      } finally {
        lock.unlock(
          toLaunch.getPendingTaskId().getRequestId(),
          LOCK_NAME,
          Optional
            .ofNullable(lockStarts.remove(toLaunch.getPendingTaskId().getId()))
            .orElse(System.currentTimeMillis())
        );
      }
    }
  }

  private double scoreOffer(
    SingularityPendingTask pendingTask,
    SingularityOfferHolder offer
  ) {
    // TODO grab from OfferScheduler class
  }
}
