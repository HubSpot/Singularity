package com.hubspot.singularity.executor;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.Status;
import org.apache.mesos.Protos.TaskID;
import org.apache.mesos.Protos.TaskState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.config.SingularityExecutorLogging;
import com.hubspot.singularity.executor.config.SingularityExecutorModule;
import com.hubspot.singularity.executor.task.SingularityExecutorTask;
import com.hubspot.singularity.executor.task.SingularityExecutorTaskProcessCallable;
import com.hubspot.singularity.executor.utils.ExecutorUtils;

@Singleton
public class SingularityExecutorMonitor {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityExecutorMonitor.class);

  private final ListeningExecutorService processBuilderPool;
  private final ListeningExecutorService runningProcessPool;
  private final ScheduledExecutorService exitChecker;

  private final Lock exitLock;
  private final AtomicBoolean alreadyShutDown;
  private final CountDownLatch latch;

  @SuppressWarnings("rawtypes")
  private volatile Optional<Future> exitCheckerFuture;
  private volatile RunState runState;

  private final SingularityExecutorConfiguration configuration;
  private final SingularityExecutorLogging logging;
  private final ExecutorUtils executorUtils;
  private final SingularityExecutorProcessKiller processKiller;
  private final SingularityExecutorThreadChecker threadChecker;

  private final Map<String, SingularityExecutorTask> tasks;
  private final Map<String, ListenableFuture<ProcessBuilder>> processBuildingTasks;
  private final Map<String, SingularityExecutorTaskProcessCallable> processRunningTasks;
  private final Map<String, ListeningExecutorService> taskToShellCommandPool;

  @Inject
  public SingularityExecutorMonitor(@Named(SingularityExecutorModule.ALREADY_SHUT_DOWN) AtomicBoolean alreadyShutDown, SingularityExecutorLogging logging, ExecutorUtils executorUtils,
      SingularityExecutorProcessKiller processKiller, SingularityExecutorThreadChecker threadChecker, SingularityExecutorConfiguration configuration) {
    this.logging = logging;
    this.configuration = configuration;
    this.executorUtils = executorUtils;
    this.processKiller = processKiller;
    this.exitChecker = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("SingularityExecutorExitChecker-%d").build());
    this.threadChecker = threadChecker;
    this.threadChecker.start(this);

    this.tasks = Maps.newConcurrentMap();
    this.processBuildingTasks = Maps.newConcurrentMap();
    this.processRunningTasks = Maps.newConcurrentMap();
    this.taskToShellCommandPool = Maps.newConcurrentMap();

    this.processBuilderPool = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("SingularityExecutorProcessBuilder-%d").build()));
    this.runningProcessPool = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("SingularityExecutorProcessRunner-%d").build()));

    this.runState = RunState.STARTING;
    this.exitLock = new ReentrantLock();
    this.alreadyShutDown = alreadyShutDown;
    this.latch = new CountDownLatch(4);
  }

  public void start(ExecutorDriver driver) {
    Preconditions.checkState(runState == RunState.STARTING);
    this.runState = RunState.RUNNING;
    this.exitCheckerFuture = Optional.of(startExitChecker(driver, configuration.getInitialIdleExecutorShutdownWaitMillis()));
  }

  public enum RunState {
    STARTING, RUNNING, SHUTDOWN;
  }

  public enum SubmitState {
    SUBMITTED, REJECTED, TASK_ALREADY_EXISTED;
  }

  public void shutdown(ExecutorDriver driver) {
    if (!alreadyShutDown.compareAndSet(false, true)) {
      LOG.info("Already ran shut down process");
      return;
    }

    LOG.info("Shutdown requested with driver {}", driver);

    threadChecker.getExecutorService().shutdown();

    processBuilderPool.shutdown();

    runningProcessPool.shutdown();

    for (SingularityExecutorTask task : tasks.values()) {
      if (!task.wasKilled()) {
        task.getLog().info("Executor shutting down - requested task kill with state: {}", requestKill(task.getTaskId()));
      }
    }

    processKiller.getExecutorService().shutdown();

    for (Entry<String, ListeningExecutorService> taskIdToShellCommandPool : taskToShellCommandPool.entrySet()) { // in case
      LOG.warn("Shutting down abandoned pool for {}", taskIdToShellCommandPool.getKey());
      taskIdToShellCommandPool.getValue().shutdown();
    }

    exitChecker.shutdown();

    final long start = System.currentTimeMillis();

    JavaUtils.awaitTerminationWithLatch(latch, "threadChecker", threadChecker.getExecutorService(), configuration.getShutdownTimeoutWaitMillis());
    JavaUtils.awaitTerminationWithLatch(latch, "processBuilder", processBuilderPool, configuration.getShutdownTimeoutWaitMillis());
    JavaUtils.awaitTerminationWithLatch(latch, "runningProcess", runningProcessPool, configuration.getShutdownTimeoutWaitMillis());
    JavaUtils.awaitTerminationWithLatch(latch, "processKiller", processKiller.getExecutorService(), configuration.getShutdownTimeoutWaitMillis());

    LOG.info("Awaiting shutdown of all thread pools for a max of {}", JavaUtils.durationFromMillis(configuration.getShutdownTimeoutWaitMillis()));

    try {
      latch.await();
    } catch (InterruptedException e) {
      LOG.warn("While awaiting shutdown of executor services", e);
    }

    LOG.info("Waited {} for shutdown of thread pools, now waiting {} before exiting...", JavaUtils.duration(start), JavaUtils.durationFromMillis(configuration.getStopDriverAfterMillis()));

    try {
      Thread.sleep(configuration.getStopDriverAfterMillis());
    } catch (Throwable t) {
      LOG.warn("While waiting to exit", t);
    }

    LOG.info("Stopping driver {}", driver);
    Status status = driver.stop();
    LOG.info("Driver stopped with status {}", status);
  }

  private void checkForExit(final ExecutorDriver driver, final long waitMillis) {
    try {
      exitLock.lockInterruptibly();
    } catch (InterruptedException e) {
      LOG.warn("Interrupted acquiring exit lock", e);
      return;
    }

    boolean shuttingDown = false;

    try {
      if (tasks.isEmpty()) {
        LOG.info("Shutting down executor due to no tasks being submitted within {}", JavaUtils.durationFromMillis(waitMillis));
        runState = RunState.SHUTDOWN;
        shuttingDown = true;
      }
    } finally {
      exitLock.unlock();
    }

    if (shuttingDown) {
      shutdown(driver);
    } else if (runState == RunState.SHUTDOWN) {
      LOG.info("Already shutting down...");
    } else {
      LOG.info("Tasks wasn't empty, exit checker doing nothing...");
    }
  }

  @SuppressWarnings("rawtypes")
  private Future startExitChecker(final ExecutorDriver driver, final long waitTimeMillis) {
    LOG.info("Starting an exit checker that will run in {}", JavaUtils.durationFromMillis(waitTimeMillis));

    return exitChecker.schedule(new Runnable() {

      @Override
      public void run() {
        LOG.info("Exit checker running...");

        try {
          checkForExit(driver, waitTimeMillis);
        } catch (Throwable t) {
          logAndExit(2, "While shutting down", t);
        }
      }
    }, waitTimeMillis, TimeUnit.MILLISECONDS);
  }

  private void clearExitCheckerUnsafe() {
    if (exitCheckerFuture.isPresent()) {
      LOG.info("Canceling an exit checker");
      exitCheckerFuture.get().cancel(true);
      exitCheckerFuture = Optional.absent();
    }
  }

  public SubmitState submit(final SingularityExecutorTask task) {
    exitLock.lock();

    try {
      final Lock taskLock = task.getLock();
      taskLock.lock();
      try {
        if (runState == RunState.SHUTDOWN) {
          finishTask(task, TaskState.TASK_LOST, "Task couldn't start because executor is shutting down", Optional.<String> absent());

          return SubmitState.REJECTED;
        }

        if (tasks.containsKey(task.getTaskId())) {
          return SubmitState.TASK_ALREADY_EXISTED;
        }
        tasks.put(task.getTaskId(), task);

        clearExitCheckerUnsafe();

        final ListenableFuture<ProcessBuilder> processBuildFuture = processBuilderPool.submit(task.getProcessBuilder());

        processBuildingTasks.put(task.getTaskId(), processBuildFuture);

        watchProcessBuilder(task, processBuildFuture);
      } finally {
        taskLock.unlock();
      }
    } finally {
      exitLock.unlock();
    }

    return SubmitState.SUBMITTED;
  }

  private void logAndExit(int statusCode, String format, Object... args) {
    try {
      LOG.error(format, args);
    } finally {
      System.exit(statusCode);
    }
  }

  public Collection<SingularityExecutorTaskProcessCallable> getRunningTasks() {
    return processRunningTasks.values();
  }

  public Optional<SingularityExecutorTaskProcessCallable> getTaskProcess(String taskId) {
    return Optional.fromNullable(processRunningTasks.get(taskId));
  }

  public Optional<SingularityExecutorTask> getTask(String taskId) {
    return Optional.fromNullable(tasks.get(taskId));
  }

  public ListeningExecutorService getShellCommandExecutorServiceForTask(String taskId) {
    if (!taskToShellCommandPool.containsKey(taskId)) {
      ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat(taskId + "-shellCommandPool-%d").build()));

      taskToShellCommandPool.put(taskId, executorService);
    }

    return taskToShellCommandPool.get(taskId);
  }

  public void finishTask(final SingularityExecutorTask task, Protos.TaskState taskState, String message, Optional<String> errorMsg, Object... errorObjects) {
    try {
      if (errorMsg.isPresent()) {
        task.getLog().error(errorMsg.get(), errorObjects);
      }
    } finally {
      try {
        sendStatusUpdate(task, taskState, message);

        onFinish(task, taskState);
      } catch (Throwable t) {
        logAndExit(3, "Failed while finishing task {} (state {})", task.getTaskId(), taskState, t);
      }
    }
  }

  private void watchProcessBuilder(final SingularityExecutorTask task, final ListenableFuture<ProcessBuilder> processBuildFuture) {
    Futures.addCallback(processBuildFuture, new FutureCallback<ProcessBuilder>() {

      private void onSuccessThrows(ProcessBuilder processBuilder) {
        task.getLog().debug("Process builder finished succesfully... ");

        boolean wasKilled = false;

        final Lock taskLock = task.getLock();
        taskLock.lock();

        try {
          processBuildingTasks.remove(task.getTaskId());

          wasKilled = task.wasKilled();

          if (!wasKilled) {
            processRunningTasks.put(task.getTaskId(), submitProcessMonitor(task, processBuilder));
          }
        } finally {
          taskLock.unlock();
        }

        if (wasKilled) {
          finishTask(task, TaskState.TASK_KILLED, "Task killed before service process started", Optional.<String> absent());
        }
      }

      // these code blocks must not throw exceptions since they are executed inside an executor. (or must be caught)
      @Override
      public void onSuccess(ProcessBuilder processBuilder) {
        try {
          onSuccessThrows(processBuilder);
        } catch (Throwable t) {
          finishTask(task, TaskState.TASK_LOST, String.format("Task lost while transitioning due to: %s", t.getClass().getSimpleName()), Optional.of("While submitting process task"), t);
        }
      }

      @Override
      public void onFailure(Throwable t) {
        TaskState state = TaskState.TASK_LOST;
        String message = String.format("%s while initializing task: %s", t.getClass().getSimpleName(), t.getMessage());

        try {
          if (task.wasKilled()) {
            state = TaskState.TASK_KILLED;
            message = String.format("Task killed, caught expected %s", t.getClass().getSimpleName());
          }
        } finally {
          finishTask(task, state, message, Optional.of("Task {} failed before starting process"), task, t);
        }
      }

    });

  }

  private void sendStatusUpdate(SingularityExecutorTask task, Protos.TaskState taskState, String message) {
    executorUtils.sendStatusUpdate(task.getDriver(), TaskID.newBuilder().setValue(task.getTaskId()).build(), taskState, message, task.getLog());
  }

  private void onFinish(SingularityExecutorTask task, Protos.TaskState taskState) {
    processKiller.cancelDestroyFuture(task.getTaskId());

    tasks.remove(task.getTaskId());
    processRunningTasks.remove(task.getTaskId());
    processBuildingTasks.remove(task.getTaskId());

    task.cleanup(taskState);

    ListeningExecutorService executorService = taskToShellCommandPool.remove(task.getTaskId());

    if (executorService != null) {
      executorService.shutdownNow();
      try {
        executorService.awaitTermination(5, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        LOG.warn("Awaiting shutdown of shell executor service", e);
      }
    }

    logging.stopTaskLogger(task.getTaskId(), task.getLogbackLog());

    checkIdleExecutorShutdown(task.getDriver());
  }

  private void checkIdleExecutorShutdown(ExecutorDriver driver) {
    exitLock.lock();

    try {
      clearExitCheckerUnsafe();

      if (tasks.isEmpty() && runState == RunState.RUNNING) {
        exitCheckerFuture = Optional.of(startExitChecker(driver, configuration.getIdleExecutorShutdownWaitMillis()));
      }
    } finally {
      exitLock.unlock();
    }
  }

  public enum KillState {
    DIDNT_EXIST, INTERRUPTING_PRE_PROCESS, KILLING_PROCESS, DESTROYING_PROCESS, INCONSISTENT_STATE;
  }

  public KillState requestKill(String taskId) {
    return requestKill(taskId, Optional.<String>absent(), false);
  }

  public KillState requestKill(String taskId, Optional<String> user, boolean destroy) {
    final Optional<SingularityExecutorTask> maybeTask = Optional.fromNullable(tasks.get(taskId));

    if (!maybeTask.isPresent()) {
      return KillState.DIDNT_EXIST;
    }

    final SingularityExecutorTask task = maybeTask.get();

    if (!destroy && task.wasForceDestroyed()) {
      task.getLog().debug("Already force destroyed, will not issue additional kill");
      return KillState.DESTROYING_PROCESS;
    }

    task.getLog().info("Executor asked to kill {}", taskId);

    ListenableFuture<ProcessBuilder> processBuilderFuture = null;
    SingularityExecutorTaskProcessCallable runningProcess = null;

    task.getLock().lock();

    boolean wasKilled = task.wasKilled();

    try {
      if (!wasKilled) {
        task.markKilled(user);
      }

      processBuilderFuture = processBuildingTasks.get(task.getTaskId());
      runningProcess = processRunningTasks.get(task.getTaskId());
    } finally {
      task.getLock().unlock();
    }

    if (processBuilderFuture != null) {
      task.getLog().info("Canceling process builder future for {}", taskId);

      CancelThread cancelThread = new CancelThread(processBuilderFuture, task);
      cancelThread.start();

      return KillState.INTERRUPTING_PRE_PROCESS;
    }

    if (runningProcess != null) {
      if (destroy) {
        if (user.isPresent()) {
          task.getLog().info("Destroying process with pid {} for task {} by request from user {}", runningProcess.getCurrentPid(), taskId, user.get());
        } else {
          task.getLog().info("Destroying process with pid {} for task {}", runningProcess.getCurrentPid(), taskId);
        }
        task.markForceDestroyed(user);
        runningProcess.signalKillToProcessIfActive();
        return KillState.DESTROYING_PROCESS;
      }

      if (processKiller.isKillInProgress(taskId)) {
        task.getLog().info("Kill already in progress for task {}", taskId);
        return KillState.KILLING_PROCESS;
      }

      if (user.isPresent()) {
        task.getLog().info("Killing process for task {} by request from {}", taskId, user.get());
      } else {
        task.getLog().info("Killing process for task {}", taskId);
      }

      processKiller.submitKillRequest(runningProcess);
      return KillState.KILLING_PROCESS;
    }

    return KillState.INCONSISTENT_STATE;
  }

  private static class CancelThread extends Thread {

    private final ListenableFuture<ProcessBuilder> processBuilderFuture;
    private final SingularityExecutorTask task;

    public CancelThread(ListenableFuture<ProcessBuilder> processBuilderFuture, SingularityExecutorTask task) {
      super("SingularityExecutorMonitor-cancel-thread");

      this.processBuilderFuture = processBuilderFuture;
      this.task = task;
    }

    @Override
    public void run() {
      processBuilderFuture.cancel(true);
      task.getProcessBuilder().cancel();
    }

  }

  private SingularityExecutorTaskProcessCallable buildProcessCallable(final SingularityExecutorTask task, ProcessBuilder processBuilder) {
    return new SingularityExecutorTaskProcessCallable(task, processBuilder, executorUtils);
  }

  private SingularityExecutorTaskProcessCallable submitProcessMonitor(final SingularityExecutorTask task, ProcessBuilder processBuilder) {
    SingularityExecutorTaskProcessCallable processCallable = buildProcessCallable(task, processBuilder);

    final ListenableFuture<Integer> processExitFuture = runningProcessPool.submit(processCallable);

    watchProcessExitFuture(task, processExitFuture);

    return processCallable;
  }

  private void watchProcessExitFuture(final SingularityExecutorTask task, final ListenableFuture<Integer> processExitFuture) {
    Futures.addCallback(processExitFuture, new FutureCallback<Integer>() {

      // these code blocks must not throw exceptions since they are executed inside an executor. (or must be caught)
      @Override
      public void onSuccess(Integer exitCode) {
        TaskState taskState = null;
        String message = null;
        Optional<String> maybeKilledBy = task.getKilledBy();

        if (task.wasKilledDueToThreads()) {
          taskState = TaskState.TASK_FAILED;

          message = String.format("Task used %s threads and was killed (max %s)", task.getThreadCountAtOverageTime(), task.getExecutorData().getMaxTaskThreads().get());
        } else if (task.wasKilled()) {
          taskState = TaskState.TASK_KILLED;

          if (task.wasDestroyedAfterWaiting()) {
            final long millisWaited = task.getExecutorData().getSigKillProcessesAfterMillis().or(configuration.getHardKillAfterMillis());

            message = String.format("Task killed forcibly after waiting at least %s", JavaUtils.durationFromMillis(millisWaited));
          } else if (task.wasForceDestroyed()) {
            if (maybeKilledBy.isPresent()) {
              message = String.format("Task killed forcibly by %s", maybeKilledBy.get());
            } else {
              message = "Task killed forcibly after multiple kill requests from framework";
            }
          } else {
            message = "Task killed. Process exited gracefully with code " + exitCode;
          }
        } else if (task.isSuccessExitCode(exitCode)) {
          taskState = TaskState.TASK_FINISHED;

          message = "Process exited normally with code " + exitCode;
        } else {
          taskState = TaskState.TASK_FAILED;

          message = "Process failed with code " + exitCode;
        }

        sendStatusUpdate(task, taskState, message);

        onFinish(task, taskState);
      }

      @Override
      public void onFailure(Throwable t) {
        task.getLog().error("Task {} failed while running process", task, t);

        TaskState taskState = null;
        String message = null;

        if (task.wasKilled()) {
          taskState = TaskState.TASK_KILLED;
          message = String.format("Task killed, caught %s", t.getClass().getSimpleName());
        } else {
          taskState = TaskState.TASK_LOST;
          message = String.format("%s while running process %s", t.getClass().getSimpleName(), t.getMessage());
        }

        sendStatusUpdate(task, taskState, message);

        onFinish(task, taskState);
      }

    });
  }

}
