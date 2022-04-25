package com.hubspot.singularity.scheduler;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DisasterManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.mesos.SingularityPendingTaskQueueProcessor;
import com.hubspot.singularity.mesos.SingularitySchedulerLock;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SingularitySchedulerPoller extends SingularityLeaderOnlyPoller {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularitySchedulerPoller.class
  );

  private final TaskManager taskManager;
  private final SingularityScheduler scheduler;
  private final DisasterManager disasterManager;
  private final SingularitySchedulerLock lock;
  private final SingularityPendingTaskQueueProcessor taskQueueProcessor;

  @Inject
  SingularitySchedulerPoller(
    TaskManager taskManager,
    SingularityScheduler scheduler,
    SingularityConfiguration configuration,
    SingularitySchedulerLock lock,
    DisasterManager disasterManager,
    SingularityPendingTaskQueueProcessor taskQueueProcessor
  ) {
    super(configuration.getCheckSchedulerEverySeconds(), TimeUnit.SECONDS);
    this.taskManager = taskManager;
    this.scheduler = scheduler;
    this.disasterManager = disasterManager;
    this.lock = lock;
    this.taskQueueProcessor = taskQueueProcessor;
  }

  @Override
  public void runActionOnPoll() {
    if (disasterManager.isDisabled(SingularityAction.RUN_SCHEDULER_POLLER)) {
      LOG.warn("Scheduler poller is disabled");
      return;
    }

    for (SingularityPendingTaskId taskId : taskManager.getPendingTasksMarkedForDeletion()) {
      lock.runWithRequestLock(
        () -> {
          taskQueueProcessor.removePendingTask(taskId);
          taskManager.deletePendingTask(taskId);
        },
        taskId.getRequestId(),
        String.format(
          "%s#%s",
          getClass().getSimpleName(),
          "checkOffers -> pendingTaskDeletes"
        )
      );
    }

    scheduler.drainPendingQueue();
    scheduler.checkForStalledTaskLaunches();
  }
}
