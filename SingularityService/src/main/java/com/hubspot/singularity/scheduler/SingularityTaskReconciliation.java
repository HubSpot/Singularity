package com.hubspot.singularity.scheduler;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.apache.mesos.v1.Protos.AgentID;
import org.apache.mesos.v1.Protos.TaskID;
import org.apache.mesos.v1.Protos.TaskState;
import org.apache.mesos.v1.Protos.TaskStatus;
import org.apache.mesos.v1.scheduler.Protos.Call.Reconcile.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.UniformReservoir;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.json.SingularityMesosTaskStatusObject;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityAbort.AbortReason;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.SingularityManagedScheduledExecutorServiceFactory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskReconciliationStatistics;
import com.hubspot.singularity.SingularityTaskStatusHolder;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.StateManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.mesos.SingularityMesosSchedulerClient;
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
  private final SingularityMesosSchedulerClient schedulerClient;
  private final StateManager stateManager;

  @Inject
  public SingularityTaskReconciliation(SingularityManagedScheduledExecutorServiceFactory executorServiceFactory,
                                       SingularityExceptionNotifier exceptionNotifier,
                                       TaskManager taskManager,
                                       StateManager stateManager,
                                       SingularityConfiguration configuration,
                                       @Named(SingularityMainModule.SERVER_ID_PROPERTY) String serverId,
                                       SingularityAbort abort,
                                       SingularityMesosSchedulerClient schedulerClient) {
    this.taskManager = taskManager;
    this.stateManager = stateManager;
    this.serverId = serverId;

    this.exceptionNotifier = exceptionNotifier;
    this.configuration = configuration;
    this.abort = abort;
    this.schedulerClient = schedulerClient;

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
    final long taskReconciliationStartedAt = System.currentTimeMillis();

    if (!isRunningReconciliation.compareAndSet(false, true)) {
      LOG.info("Reconciliation is already running, NOT starting a new reconciliation process");
      return ReconciliationState.ALREADY_RUNNING;
    }

    if (!schedulerClient.isRunning()) {
      LOG.trace("Not running reconciliation - no active scheduler present");
      isRunningReconciliation.set(false);
      return ReconciliationState.NO_DRIVER;
    }

    final List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();

    LOG.info("Starting a reconciliation cycle - {} current active tasks", activeTaskIds.size());

    schedulerClient.reconcile(Collections.emptyList());

    scheduleReconciliationCheck(taskReconciliationStartedAt, activeTaskIds, 0, new Histogram(new UniformReservoir()));

    return ReconciliationState.STARTED;
  }

  private void scheduleReconciliationCheck(final long reconciliationStart, final Collection<SingularityTaskId> remainingTaskIds, final int numTimes, final Histogram histogram) {
    LOG.info("Scheduling reconciliation check #{} - {} tasks left - waiting {}", numTimes + 1, remainingTaskIds.size(), JavaUtils.durationFromMillis(configuration.getCheckReconcileWhenRunningEveryMillis()));

    executorService.schedule(new Runnable() {

      @Override
      public void run() {
        try {
          checkReconciliation(reconciliationStart, remainingTaskIds, numTimes + 1, histogram);
        } catch (Throwable t) {
          LOG.error("While checking for reconciliation tasks", t);
          exceptionNotifier.notify(String.format("Error checking for reconciliation tasks (%s)", t.getMessage()), t);
          abort.abort(AbortReason.UNRECOVERABLE_ERROR, Optional.of(t));
        }
      }
    }, configuration.getCheckReconcileWhenRunningEveryMillis(), TimeUnit.MILLISECONDS);
  }

  private void checkReconciliation(final long reconciliationStart, final Collection<SingularityTaskId> remainingTaskIds, final int numTimes, final Histogram histogram) {
    final List<SingularityTaskStatusHolder> taskStatusHolders = taskManager.getLastActiveTaskStatusesFor(remainingTaskIds);
    final List<SingularityMesosTaskStatusObject> taskStatuses = Lists.newArrayListWithCapacity(taskStatusHolders.size());

    for (SingularityTaskStatusHolder taskStatusHolder : taskStatusHolders) {
      if (taskStatusHolder.getServerId().equals(serverId) && taskStatusHolder.getServerTimestamp() > reconciliationStart) {
        histogram.update(taskStatusHolder.getServerTimestamp() - reconciliationStart);
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
          fakeTaskStatusBuilder.setAgentId(AgentID.newBuilder().setValue(taskStatusHolder.getSlaveId().get()));
        }

        LOG.info("Task {} didn't have a TaskStatus yet, submitting fake status", taskStatusHolder.getTaskId());
        taskStatuses.add(SingularityMesosTaskStatusObject.fromProtos(fakeTaskStatusBuilder.build()));
      }
    }

    if (taskStatuses.isEmpty()) {
      LOG.info("Task reconciliation ended after {} checks and {}", numTimes, JavaUtils.duration(reconciliationStart));

      final Snapshot snapshot = histogram.getSnapshot();
      stateManager.saveTaskReconciliationStatistics(new SingularityTaskReconciliationStatistics(reconciliationStart, System.currentTimeMillis() - reconciliationStart, numTimes, histogram.getCount(), snapshot.getMax(), snapshot.getMean(), snapshot.getMin(), snapshot.getMedian(), snapshot.get75thPercentile(), snapshot.get95thPercentile(), snapshot.get98thPercentile(), snapshot.get99thPercentile(), snapshot.get999thPercentile(), snapshot.getStdDev()));

      isRunningReconciliation.set(false);

      return;
    }

    LOG.info("Requesting reconciliation of {} taskStatuses, task reconciliation has been running for {}", taskStatuses.size(), JavaUtils.duration(reconciliationStart));

    schedulerClient.reconcile(taskStatuses.stream().map((t) -> Task.newBuilder().setTaskId(t.getTaskId()).setAgentId(t.getAgentId()).build()).collect(Collectors.toList()));

    scheduleReconciliationCheck(reconciliationStart, remainingTaskIds, numTimes, histogram);
  }
}
