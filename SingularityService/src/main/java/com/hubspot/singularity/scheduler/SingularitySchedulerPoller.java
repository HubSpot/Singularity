package com.hubspot.singularity.scheduler;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DisasterManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.mesos.SingularityMesosOfferScheduler;
import com.hubspot.singularity.mesos.SingularitySchedulerLock;

@Singleton
public class SingularitySchedulerPoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularitySchedulerPoller.class);

  private final SingularityMesosOfferScheduler offerScheduler;
  private final TaskManager taskManager;
  private final SingularityScheduler scheduler;
  private final DisasterManager disasterManager;
  private final SingularitySchedulerLock lock;

  @Inject
  SingularitySchedulerPoller(SingularityMesosOfferScheduler offerScheduler, TaskManager taskManager, SingularityScheduler scheduler,
                             SingularityConfiguration configuration, SingularitySchedulerLock lock, DisasterManager disasterManager) {
    super(configuration.getCheckSchedulerEverySeconds(), TimeUnit.SECONDS);

    this.offerScheduler = offerScheduler;
    this.taskManager = taskManager;
    this.scheduler = scheduler;
    this.disasterManager = disasterManager;
    this.lock = lock;
  }

  @Override
  public void runActionOnPoll() {
    if (disasterManager.isDisabled(SingularityAction.RUN_SCHEDULER_POLLER)) {
      LOG.warn("Scheduler poller is disabled");
      return;
    }

    lock.runWithOffersLock(() -> {
      for (SingularityPendingTaskId taskId : taskManager.getPendingTasksMarkedForDeletion()) {
        lock.runWithRequestLock(() -> taskManager.deletePendingTask(taskId), taskId.getRequestId(), String.format("%s#%s", getClass().getSimpleName(), "checkOffers -> pendingTaskDeletes"));
      }

      scheduler.drainPendingQueue();

      // Check against only cached offers
      offerScheduler.resourceOffers(Collections.emptyList());
    }, "SingularitySchedulerPoller");
  }
}
