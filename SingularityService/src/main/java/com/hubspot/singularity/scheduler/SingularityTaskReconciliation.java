package com.hubspot.singularity.scheduler;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Singleton;

import org.apache.mesos.Protos.SlaveID;
import org.apache.mesos.Protos.TaskID;
import org.apache.mesos.Protos.TaskState;
import org.apache.mesos.Protos.TaskStatus;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityAbort.AbortReason;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.SingularityManagedScheduledExecutorServiceFactory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskStatusHolder;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.mesos.SchedulerDriverSupplier;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

@Singleton
public class SingularityTaskReconciliation {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityTaskReconciliation.class);

  private final TaskManager taskManager;
  private final String serverId;
  private final ScheduledExecutorService executorService;
  private final AtomicBoolean isRunningReconciliation;
  private final SingularityConfiguration configuration;
  private final SingularityAbort abort;
  private final SingularityExceptionNotifier exceptionNotifier;
  private final SchedulerDriverSupplier schedulerDriverSupplier;

  @Inject
  public SingularityTaskReconciliation(SingularityManagedScheduledExecutorServiceFactory executorServiceFactory,
      SingularityExceptionNotifier exceptionNotifier,
      TaskManager taskManager,
      SingularityConfiguration configuration,
      @Named(SingularityMainModule.SERVER_ID_PROPERTY) String serverId,
      SingularityAbort abort,
      SchedulerDriverSupplier schedulerDriverSupplier) {
    this.taskManager = taskManager;
    this.serverId = serverId;

    this.exceptionNotifier = exceptionNotifier;
    this.configuration = configuration;
    this.abort = abort;
    this.schedulerDriverSupplier = schedulerDriverSupplier;

    this.isRunningReconciliation = new AtomicBoolean(false);
    this.executorService = executorServiceFactory.get(getClass().getSimpleName());
  }

  enum ReconciliationState {
    ALREADY_RUNNING, STARTED, NO_DRIVER;
  }

  @VisibleForTesting
  boolean isReconciliationRunning() {
    return isRunningReconciliation.get();
  }

  public ReconciliationState startReconciliation() {
    if (!isRunningReconciliation.compareAndSet(false, true)) {
      LOG.info("Reconciliation is already running, NOT starting a new reconciliation process");
      return ReconciliationState.ALREADY_RUNNING;
    }

    Optional<SchedulerDriver> schedulerDriver = schedulerDriverSupplier.get();

    if (!schedulerDriver.isPresent()) {
      LOG.trace("Not running reconciliation - no schedulerDriver present");
      isRunningReconciliation.set(false);
      return ReconciliationState.NO_DRIVER;
    }

    final long reconciliationStart = System.currentTimeMillis();
    final List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();

    LOG.info("Starting a reconciliation cycle - {} current active tasks", activeTaskIds.size());

    SchedulerDriver driver = schedulerDriver.get();
    driver.reconcileTasks(Collections.<TaskStatus> emptyList());

    scheduleReconciliationCheck(driver, reconciliationStart, activeTaskIds, 0);

    return ReconciliationState.STARTED;
  }

  private void scheduleReconciliationCheck(final SchedulerDriver driver, final long reconciliationStart, final Collection<SingularityTaskId> remainingTaskIds, final int numTimes) {
    LOG.info("Scheduling reconciliation check #{} - {} tasks left - waiting {}", numTimes + 1, remainingTaskIds.size(), JavaUtils.durationFromMillis(configuration.getCheckReconcileWhenRunningEveryMillis()));

    executorService.schedule(new Runnable() {

      @Override
      public void run() {
        try {
          checkReconciliation(driver, reconciliationStart, remainingTaskIds, numTimes + 1);
        } catch (Throwable t) {
          LOG.error("While checking for reconciliation tasks", t);
          exceptionNotifier.notify(t, Collections.<String, String>emptyMap());
          abort.abort(AbortReason.UNRECOVERABLE_ERROR, Optional.of(t));
        }
      }
    }, configuration.getCheckReconcileWhenRunningEveryMillis(), TimeUnit.MILLISECONDS);
  }

  private void checkReconciliation(final SchedulerDriver driver, final long reconciliationStart, final Collection<SingularityTaskId> remainingTaskIds, final int numTimes) {
    final List<SingularityTaskStatusHolder> taskStatusHolders = taskManager.getLastActiveTaskStatusesFor(remainingTaskIds);
    final List<TaskStatus> taskStatuses = Lists.newArrayListWithCapacity(taskStatusHolders.size());

    for (SingularityTaskStatusHolder taskStatusHolder : taskStatusHolders) {
      if (taskStatusHolder.getServerId().equals(serverId) && taskStatusHolder.getServerTimestamp() > reconciliationStart) {
        continue;
      }

      if (taskStatusHolder.getTaskStatus().isPresent()) {
        LOG.debug("Re-requesting task status for {}", taskStatusHolder.getTaskId());
        taskStatuses.add(taskStatusHolder.getTaskStatus().get());
      } else {
        TaskStatus.Builder fakeTaskStatusBuilder = TaskStatus.newBuilder()
            .setTaskId(TaskID.newBuilder().setValue(taskStatusHolder.getTaskId().getId()))
            .setState(TaskState.TASK_STARTING);

        if (taskStatusHolder.getSlaveId().isPresent()) {
          fakeTaskStatusBuilder.setSlaveId(SlaveID.newBuilder().setValue(taskStatusHolder.getSlaveId().get()));
        }

        LOG.info("Task {} didn't have a TaskStatus yet, submitting fake status", taskStatusHolder.getTaskId());
        taskStatuses.add(fakeTaskStatusBuilder.build());
      }
    }

    if (taskStatuses.isEmpty()) {
      LOG.info("Task reconciliation ended after {} checks and {}", numTimes, JavaUtils.duration(reconciliationStart));

      isRunningReconciliation.set(false);

      return;
    }

    LOG.info("Requesting reconciliation of {} taskStatuses, task reconciliation has been running for {}", taskStatuses.size(), JavaUtils.duration(reconciliationStart));

    driver.reconcileTasks(taskStatuses);

    scheduleReconciliationCheck(driver, reconciliationStart, remainingTaskIds, numTimes);
  }
}
