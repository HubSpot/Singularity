package com.hubspot.singularity.scheduler;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mesos.Protos.TaskStatus;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityCloseable;
import com.hubspot.singularity.SingularityCloser;
import com.hubspot.singularity.SingularityServiceModule;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskStatusHolder;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

public class SingularityTaskReconciliation extends SingularityCloseable<ScheduledExecutorService> {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityTaskReconciliation.class);

  private final TaskManager taskManager;
  private final String serverId;
  private final ScheduledExecutorService executorService;
  private final AtomicBoolean isRunningReconciliation;
  private final SingularityConfiguration configuration;
  private final SingularityAbort abort;
  private final SingularityExceptionNotifier exceptionNotifier;

  @Inject
  public SingularityTaskReconciliation(SingularityCloser closer, SingularityExceptionNotifier exceptionNotifier, TaskManager taskManager, SingularityConfiguration configuration,
      @Named(SingularityServiceModule.SERVER_ID_PROPERTY) String serverId, SingularityAbort abort) {
    super(closer);

    this.taskManager = taskManager;
    this.serverId = serverId;

    this.exceptionNotifier = exceptionNotifier;
    this.configuration = configuration;
    this.abort = abort;

    this.isRunningReconciliation = new AtomicBoolean(false);
    this.executorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("SingularityTaskReconciliation-%d").build());
  }

  @Override
  public Optional<ScheduledExecutorService> getExecutorService() {
    return Optional.of(executorService);
  }

  public void startReconciliation(SchedulerDriver driver) {
    synchronized (isRunningReconciliation) {
      if (isRunningReconciliation.get()) {
        LOG.info("Reconciliation is already running, NOT starting a new reconciliation process");
        return;
      }

      isRunningReconciliation.set(true);
    }

    final long reconciliationStart = System.currentTimeMillis();
    final List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();

    driver.reconcileTasks(Collections.<TaskStatus> emptyList());

    scheduleReconciliationCheck(driver, reconciliationStart, activeTaskIds, 0);
  }

  private void scheduleReconciliationCheck(final SchedulerDriver driver, final long reconciliationStart, final Collection<SingularityTaskId> remainingTaskIds, final int numTimes) {
    executorService.schedule(new Runnable() {

      @Override
      public void run() {
        try {
          checkReconciliation(driver, reconciliationStart, remainingTaskIds, numTimes + 1);
        } catch (Throwable t) {
          LOG.error("While checking for reconciliation tasks", t);
          exceptionNotifier.notify(t);
          abort.abort();
        }
      }
    }, configuration.getCheckReconcileWhenRunningEverySeconds(), TimeUnit.SECONDS);
  }

  private void checkReconciliation(final SchedulerDriver driver, final long reconciliationStart, final Collection<SingularityTaskId> remainingTaskIds, final int numTimes) {
    final List<SingularityTaskStatusHolder> taskStatusHolders = taskManager.getLastActiveTaskStatusesFor(remainingTaskIds);
    final List<TaskStatus> taskStatuses = Lists.newArrayListWithCapacity(taskStatusHolders.size());

    for (SingularityTaskStatusHolder taskStatusHolder : taskStatusHolders) {
      if (taskStatusHolder.getServerId().equals(serverId) && taskStatusHolder.getServerTimestamp() > reconciliationStart) {
        continue;
      }

      if (taskStatusHolder.getTaskStatus().isPresent()) {
        taskStatuses.add(taskStatusHolder.getTaskStatus().get());
      } else {
        LOG.warn("Task {} doesn't have a TaskStatus yet, can't re-request reconciliation", taskStatusHolder.getTaskId());
      }
    }

    if (taskStatuses.isEmpty()) {
      LOG.info("Task reconciliation ended after {}", JavaUtils.duration(reconciliationStart));

      isRunningReconciliation.set(false);

      return;
    }

    LOG.info("Requesting reconciliation of {} taskStatuses, task reconciliation has been running for {}", taskStatuses.size(), JavaUtils.duration(reconciliationStart));

    driver.reconcileTasks(taskStatuses);

    scheduleReconciliationCheck(driver, reconciliationStart, remainingTaskIds, numTimes);
  }

}
