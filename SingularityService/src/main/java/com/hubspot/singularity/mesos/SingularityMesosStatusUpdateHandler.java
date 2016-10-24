package com.hubspot.singularity.mesos;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.TaskState;
import org.apache.mesos.Protos.TaskStatus.Reason;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Multiset;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.InvalidSingularityTaskIdException;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityAbort.AbortReason;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskStatusHolder;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.transcoders.IdTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTranscoderException;
import com.hubspot.singularity.scheduler.SingularityHealthchecker;
import com.hubspot.singularity.scheduler.SingularityNewTaskChecker;
import com.hubspot.singularity.scheduler.SingularityScheduler;
import com.hubspot.singularity.scheduler.SingularitySchedulerStateCache;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

import io.dropwizard.lifecycle.Managed;

@Singleton
public class SingularityMesosStatusUpdateHandler implements Managed {
    private static final Logger LOG = LoggerFactory.getLogger(SingularityMesosStatusUpdateHandler.class);

    private final TaskManager taskManager;
    private final DeployManager deployManager;
    private final RequestManager requestManager;
    private final IdTranscoder<SingularityTaskId> taskIdTranscoder;
    private final SingularityExceptionNotifier exceptionNotifier;
    private final SingularityHealthchecker healthchecker;
    private final SingularityNewTaskChecker newTaskChecker;
    private final SingularitySlaveAndRackManager slaveAndRackManager;
    private final SingularityLogSupport logSupport;
    private final SingularityScheduler scheduler;
    private final Provider<SingularitySchedulerStateCache> stateCacheProvider;
    private final String serverId;
    private final BlockingQueue<Protos.TaskStatus> statusUpdateQueue;
    private final ExecutorService executorService;
    private final SchedulerDriverSupplier schedulerDriverSupplier;
    private final AtomicBoolean handlerStarted;
    private final Lock schedulerLock;
    private final boolean processStatusUpdatesInSeparateThread;
    private final SingularityAbort singularityAbort;
    private final SingularityConfiguration configuration;
    private final Multiset<Protos.TaskStatus.Reason> taskLostReasons;

    private Future statusUpdateFuture;

    @Inject
    public SingularityMesosStatusUpdateHandler(TaskManager taskManager, DeployManager deployManager, RequestManager requestManager,
        IdTranscoder<SingularityTaskId> taskIdTranscoder, SingularityExceptionNotifier exceptionNotifier, SingularityHealthchecker healthchecker,
        SingularityNewTaskChecker newTaskChecker, SingularitySlaveAndRackManager slaveAndRackManager, SingularityLogSupport logSupport, SingularityScheduler scheduler,
        Provider<SingularitySchedulerStateCache> stateCacheProvider, @Named(SingularityMainModule.SERVER_ID_PROPERTY) String serverId,
        SchedulerDriverSupplier schedulerDriverSupplier,
        @Named(SingularityMesosModule.SCHEDULER_LOCK_NAME) final Lock schedulerLock,
        @Named(SingularityMainModule.STATUS_UPDATE_THREADPOOL_NAME) ScheduledExecutorService executorService,
        SingularityConfiguration configuration,
        SingularityAbort singularityAbort,
        @Named(SingularityMesosModule.TASK_LOST_REASONS_COUNTER) Multiset<Protos.TaskStatus.Reason> taskLostReasons) {
        this.taskManager = taskManager;
        this.deployManager = deployManager;
        this.requestManager = requestManager;
        this.taskIdTranscoder = taskIdTranscoder;
        this.exceptionNotifier = exceptionNotifier;
        this.healthchecker = healthchecker;
        this.newTaskChecker = newTaskChecker;
        this.slaveAndRackManager = slaveAndRackManager;
        this.logSupport = logSupport;
        this.scheduler = scheduler;
        this.stateCacheProvider = stateCacheProvider;
        this.serverId = serverId;
        this.schedulerDriverSupplier = schedulerDriverSupplier;
        this.schedulerLock = schedulerLock;
        this.singularityAbort = singularityAbort;
        this.configuration = configuration;
        this.taskLostReasons = taskLostReasons;
        this.handlerStarted = new AtomicBoolean();

        this.statusUpdateQueue = new ArrayBlockingQueue<>(configuration.getStatusUpdateQueueCapacity());
        this.executorService = executorService;
        this.processStatusUpdatesInSeparateThread = configuration.isProcessStatusUpdatesInSeparateThread();
    }

    /**
     * 1- we have a previous update, and this is a duplicate of it (ignore) 2- we don't have a
     * previous update, 2 cases: a - this task has already been destroyed (we can ignore it then) b -
     * we've never heard of this task (very unlikely since we first write a status into zk before we
     * launch a task)
     */
    private boolean isDuplicateOrIgnorableStatusUpdate(Optional<SingularityTaskStatusHolder> previousTaskStatusHolder, final SingularityTaskStatusHolder newTaskStatusHolder) {
        if (!previousTaskStatusHolder.isPresent()) {
            return true;
        }

        if (!previousTaskStatusHolder.get().getTaskStatus().isPresent()) { // this is our launch state
            return false;
        }

        return previousTaskStatusHolder.get().getTaskStatus().get().getState() == newTaskStatusHolder.getTaskStatus().get().getState();
    }

    private void saveNewTaskStatusHolder(SingularityTaskId taskIdObj, SingularityTaskStatusHolder newTaskStatusHolder, ExtendedTaskState taskState) {
        if (taskState.isDone()) {
            taskManager.deleteLastActiveTaskStatus(taskIdObj);
        } else {
            taskManager.saveLastActiveTaskStatus(newTaskStatusHolder);
        }
    }

    private Optional<SingularityTaskId> getTaskId(String taskId) {
        try {
            return Optional.of(taskIdTranscoder.fromString(taskId));
        } catch (InvalidSingularityTaskIdException | SingularityTranscoderException e) {
            exceptionNotifier.notify(String.format("Unexpected taskId %s", taskId), e);
            LOG.error("Unexpected taskId {} ", taskId, e);
            return Optional.absent();
        }
    }

    private Optional<String> getStatusMessage(Protos.TaskStatus status, Optional<SingularityTask> task) {
        if (status.hasMessage() && !Strings.isNullOrEmpty(status.getMessage())) {
            return Optional.of(status.getMessage());
        } else if (status.hasReason() && status.getReason() == Reason.REASON_CONTAINER_LIMITATION_MEMORY) {
            if (task.isPresent() && task.get().getTaskRequest().getDeploy().getResources().isPresent()) {
                if (task.get().getTaskRequest().getDeploy().getResources().get().getDiskMb() > 0) {
                    return Optional.of(String.format("Task exceeded one or more memory limits (%s MB mem, %s MB disk).", task.get().getTaskRequest().getDeploy().getResources().get().getMemoryMb(), task.get().getTaskRequest().getDeploy().getResources().get().getDiskMb()));
                } else {
                    return Optional.of(String.format("Task exceeded memory limit (%s MB mem).", task.get().getTaskRequest().getDeploy().getResources().get().getMemoryMb()));
                }

            }
            return Optional.of("Task exceeded memory limit");
        }

        return Optional.absent();
    }

    private void updateDisasterStats(Protos.TaskStatus status) {
        if (status.getState() == TaskState.TASK_LOST) {
            taskLostReasons.add(status.getReason());
        }
    }

    private SchedulerDriver getSchedulerDriver() {
        final Optional<SchedulerDriver> maybeSchedulerDriver = schedulerDriverSupplier.get();

        if (!maybeSchedulerDriver.isPresent()) {
            throw new RuntimeException("scheduler driver not present!");
            // TODO: how best to handle?
        }

        return maybeSchedulerDriver.get();
    }

    private void unsafeProcessStatusUpdate(Protos.TaskStatus status) {
        final String taskId = status.getTaskId().getValue();

        long timestamp = System.currentTimeMillis();

        if (status.hasTimestamp()) {
            timestamp = (long) (status.getTimestamp() * 1000);
        }

        LOG.debug("Task {} is now {} ({}) at {} ", taskId, status.getState(), status.getMessage(), timestamp);

        final Optional<SingularityTaskId> maybeTaskId = getTaskId(taskId);

        if (!maybeTaskId.isPresent()) {
            getSchedulerDriver().acknowledgeStatusUpdate(status);
            return;
        }

        final SingularityTaskId taskIdObj = maybeTaskId.get();

        final SingularityTaskStatusHolder newTaskStatusHolder = new SingularityTaskStatusHolder(taskIdObj, Optional.of(status), System.currentTimeMillis(), serverId, Optional.<String>absent());
        final Optional<SingularityTaskStatusHolder> previousTaskStatusHolder = taskManager.getLastActiveTaskStatus(taskIdObj);
        final ExtendedTaskState taskState = ExtendedTaskState.fromTaskState(status.getState());

        if (isDuplicateOrIgnorableStatusUpdate(previousTaskStatusHolder, newTaskStatusHolder)) {
            LOG.trace("Ignoring status update {} to {}", taskState, taskIdObj);
            saveNewTaskStatusHolder(taskIdObj, newTaskStatusHolder, taskState);
            getSchedulerDriver().acknowledgeStatusUpdate(status);
            return;
        }

        if (configuration.getDisasterDetection().isEnabled()) {
            updateDisasterStats(status);
        }

        final Optional<SingularityTask> task = taskManager.getTask(taskIdObj);

        final boolean isActiveTask = taskManager.isActiveTask(taskId);

        if (isActiveTask && !taskState.isDone()) {
            if (task.isPresent()) {
                final Optional<SingularityPendingDeploy> pendingDeploy = deployManager.getPendingDeploy(taskIdObj.getRequestId());

                Optional<SingularityRequestWithState> requestWithState = Optional.absent();

                if (taskState == ExtendedTaskState.TASK_RUNNING) {
                    requestWithState = requestManager.getRequest(taskIdObj.getRequestId());
                    healthchecker.enqueueHealthcheck(task.get(), pendingDeploy, requestWithState);
                }

                if (!pendingDeploy.isPresent() || !pendingDeploy.get().getDeployMarker().getDeployId().equals(taskIdObj.getDeployId())) {
                    if (!requestWithState.isPresent()) {
                        requestWithState = requestManager.getRequest(taskIdObj.getRequestId());
                    }
                    newTaskChecker.enqueueNewTaskCheck(task.get(), requestWithState, healthchecker);
                }
            } else {
                final String message = String.format("Task %s is active but is missing task data", taskId);
                exceptionNotifier.notify(message);
                LOG.error(message);
            }
        }

        final Optional<String> statusMessage = getStatusMessage(status, task);

        final SingularityTaskHistoryUpdate taskUpdate =
            new SingularityTaskHistoryUpdate(taskIdObj, timestamp, taskState, statusMessage, status.hasReason() ? Optional.of(status.getReason().name()) : Optional.<String>absent());
        final SingularityCreateResult taskHistoryUpdateCreateResult = taskManager.saveTaskHistoryUpdate(taskUpdate);

        logSupport.checkDirectory(taskIdObj);

        if (taskState.isDone()) {
            healthchecker.cancelHealthcheck(taskId);
            newTaskChecker.cancelNewTaskCheck(taskId);

            taskManager.deleteKilledRecord(taskIdObj);

            SingularitySchedulerStateCache stateCache = stateCacheProvider.get();

            slaveAndRackManager.checkStateAfterFinishedTask(taskIdObj, status.getSlaveId().getValue(), stateCache);

            scheduler.handleCompletedTask(task, taskIdObj, isActiveTask, timestamp, taskState, taskHistoryUpdateCreateResult, stateCache, status);
        }

        saveNewTaskStatusHolder(taskIdObj, newTaskStatusHolder, taskState);
        getSchedulerDriver().acknowledgeStatusUpdate(status);
    }

    @Timed
    public void processStatusUpdate(Protos.TaskStatus status) {
        schedulerLock.lock();
        try {
            unsafeProcessStatusUpdate(status);
        } finally {
            schedulerLock.unlock();
        }
    }

    public void enqueueStatusUpdate(Protos.TaskStatus status) {
        if (processStatusUpdatesInSeparateThread) {
            try {
                if (statusUpdateFuture == null || statusUpdateFuture.isDone()) {
                    singularityAbort.abort(AbortReason.NO_RUNNING_STATUS_UPDATE_THREAD, Optional.<Throwable>absent());
                }
                statusUpdateQueue.put(status);
            } catch (InterruptedException ie) {
                // If we do not ack the status update it will be resent, can log this and move on
                LOG.error("Interrupted while adding status update to queue", ie);
            }
        } else {
            processStatusUpdate(status);
        }
    }

    @Override
    public void start() {
        if (!processStatusUpdatesInSeparateThread) {
            return;
        }

        if (handlerStarted.getAndSet(true)) {
            LOG.warn("StatusUpdateHandler already started!");
            return;
        }

        statusUpdateFuture = executorService.submit(new Runnable() {
            @Override
            public void run() {
                LOG.info("Status update handler thread started");
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        final Protos.TaskStatus status = statusUpdateQueue.take();
                        LOG.info("Handling status update for {} {}", status.getTaskId().getValue(), status.getState());
                        processStatusUpdate(status);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Throwable t) {
                        LOG.error("Caught exception in status update handler thread", t);
                        singularityAbort.abort(SingularityAbort.AbortReason.UNRECOVERABLE_ERROR, Optional.of(t));
                    }
                }
            }
        });
    }

    @Override
    public void stop() throws Exception {
        // noop
    }
}
